## Latest Commit: Persistent I/O Migration & Imperative Code Refactor

### Summary
Migrated from flat `extras/` and `logs/` to structured `server_data/` hierarchy with nested device logs. Implemented file-based persistent device ID allocation, per-user client output directories, synchronized state persistence, and refactored all functional programming constructs to imperative style for readability.

### New/Modified Methods in ServerCommandHandler

#### Device Log Path Resolution
- **`getDeviceLogPath(String house, String device): Path`**
  - Resolves nested device log path: `server_data/homes/<house>/<section>/<device>.csv`
  - Uses `deviceSection()` to extract section letter from device name
  - Called from `updateDeviceLogs()` and `RH_sendFile()`

#### Persistent Device Counter Allocation
- **`allocateNextDeviceName(String house, String sectionLetter): String`** [SYNCHRONIZED]
  - Reads `server_data/metadata/section_counters.txt` atomically
  - Parses current counters for the given house (format: `house:M=2,G=1,L=3,...`)
  - Increments section counter and generates device name (e.g., `M2` → `M3`)
  - Persists updated counters back to file immediately
  - Called by `registerDevice()` on every RD command
  - **Ensures**: Device IDs persist across server restarts; no duplicates even under concurrent RD requests

#### Counter Parsing Helper
- **`parseCounters(String countersStr): Map<String, Integer>`**
  - Parses counter string (e.g., `"M=2,G=1,L=1,P=1,B=1,E=1"`)
  - Returns map with section → next_id
  - Handles missing/invalid sections gracefully (initializes to 1)

#### Device State Persistence
- **`persistLastCommandValue(String house, String device, int value)`** [SYNCHRONIZED]
  - Reads `server_data/states/last_commands.txt`
  - Updates or appends `<house>:<device>:<value>` line
  - Persists atomically to file
  - Called by `registerDevice()` (initial state=0) and `sendInt()` (after EC command)
  - **Ensures**: Device values survive server restart

#### Access Control Helper
- **`hasAnyAccessibleDevice(Map devices, String house, String user): boolean`**
  - Checks if user has access to at least one device in the house
  - Replaces stream `.noneMatch()` with explicit iteration loop
  - Used by RT and LS commands to return NODATA when appropriate

### Changes to ServerCommandHandler

#### Command Dispatch Refactoring
- **`handleCommand()` method**
  - Replaced arrow-switch (`case "LS" -> { ... }`) with classic switch/case/break
  - Maintains same functionality; improves readability

#### Device Registration Refactoring
- **`registerDevice()` method**
  - **OLD**: Counted existing devices in memory to determine next ID (`count = devices.filter(section).size(); deviceID = section + (count+1)`)
  - **NEW**: Calls `allocateNextDeviceName()` which reads persistent counters from file
  - Now calls `persistLastCommandValue()` to initialize device state to 0
  - **Impact**: Device IDs persist; fixed race condition where rapid RD calls could create duplicate IDs

#### EC State Tracking
- **`sendInt()` method**
  - Now calls `persistLastCommandValue()` after updating device value
  - **Impact**: Device values survive server restart; RT can show accurate status

#### RT Command Refactoring
- **`RT()` method**
  - Replaced stream `.noneMatch(d -> hasDeviceAccess(...))` with `hasAnyAccessibleDevice()` call
  - Same logic; imperative style

#### RT File Sending Refactoring
- **`RT_sendFile()` method**
  - Replaced stream `.map().forEach()` chain with explicit for-loop
  - Uses `getDeviceLogPath()` to resolve nested log paths
  - Handles missing device logs gracefully (skips with `continue`)

#### LS Command Refactoring
- **`lsCommand()` method**
  - House listing: replaced stream `.filter().forEach()` with explicit for-loop
  - Device listing: replaced stream `.filter().forEach()` with explicit for-loop
  - Same filtering logic; improved readability

#### Permission Snapshot Refactoring
- **`createHouse()` method**
  - Replaced `permissions.computeIfAbsent(house, k -> new HashMap<>())` with explicit get/check/put
  - Same semantics; clearer intent

### Changes to ServerFileManager

#### Startup Bootstrap
- **`ensurePersistenceStructure()` method** [NEW]
  - Called from constructor automatically
  - Creates directory structure: `server_data/auth`, `server_data/metadata`, `server_data/states`, `server_data/homes`
  - Creates placeholder files with proper headers
  - **Called before any state loading**; ensures infrastructure ready at startup

#### File Path Initialization
- **`initializeFilePaths()` method**
  - Updated all paths from `extras/` and `logs/` to `server_data/` hierarchy:
    - `users.txt` → `server_data/auth/users.txt`
    - `attestation.txt` → `server_data/auth/attestation.txt`
    - `homes_registry.txt` → `server_data/metadata/homes_registry.txt`
    - `permissions.txt` → `server_data/metadata/permissions.txt`
    - `house_metadata.txt` → `server_data/metadata/house_metadata.txt`
    - `section_counters.txt` → `server_data/metadata/section_counters.txt`
    - `last_commands.txt` → `server_data/states/last_commands.txt`
    - Device logs → `server_data/homes/<house>/<section>/`

#### State Rebuild Refactoring
- **`rebuildDeviceStateFromPersistence()` method**
  - **OLD**: Used `Files.walk() + Stream API` with filter/forEach chains
  - **NEW**: Uses `Files.walkFileTree() + SimpleFileVisitor` pattern
  - Extracts house and device from nested path structure automatically
  - Same functional result; clearer imperative traversal

#### Project Root Detection
- **`findProjectRoot()` method**
  - Updated search criterion: looks for `Makefile` + `src/` directory instead of `extras/Users.txt`
  - More robust (doesn't depend on specific files existing)

### Changes to SpertaClient

#### User-Scoped Output Paths
- **`commandLoop()` method**
  - **SIGNATURE CHANGE**: Now accepts `userId` parameter
  - RT output: `logs/RT_<house>.txt` → `client_data/<userId>/status_<house>.txt`
  - RH output: `logs/RH_<house>_<device>.csv` → `client_data/<userId>/history_<device>_<house>.csv`
  - **Impact**: Multiple users don't overwrite each other's downloads

#### Code Style Refactoring
- Shutdown hook: replaced `new Thread(() -> { ... })` with explicit `new Runnable() { @Override public void run() { ... } }`

### Changes to SpertaServer

#### Startup Verification
- **`main()` method**
  - Added `verifyPersistenceStructure()` call before state initialization
  - **Impact**: Server startup verifies directory structure is ready

#### Code Style Refactoring
- Shutdown hook: replaced lambda with explicit Runnable class

### Changes to Makefile

#### Attestation Path
- Now writes to `server_data/auth/attestation.txt` (was `extras/attestation.txt`)
- Added `mkdir -p server_data/auth` before write
- **Target**: Both `compile` and `compile_client` targets updated

### New File Locations
- `server_data/auth/users.txt` - user credentials
- `server_data/auth/attestation.txt` - client hash attestation
- `server_data/metadata/homes_registry.txt` - house ownership
- `server_data/metadata/permissions.txt` - user → house/section permissions
- `server_data/metadata/house_metadata.txt` - house creation metadata
- `server_data/metadata/section_counters.txt` - device counter state (e.g., `casa:M=2,G=1,...`)
- `server_data/metadata/houses.log` - audit trail of CREATE commands
- `server_data/states/last_commands.txt` - latest device values (e.g., `casa:M1:45`)
- `server_data/homes/<house>/<section>/<device>.csv` - device operation history (nested)
- `client_data/<userId>/status_<house>.txt` - RT command results
- `client_data/<userId>/history_<device>_<house>.csv` - RH command results

### Synchronization & Thread Safety
Three critical methods use `synchronized` modifier to ensure atomic file operations under concurrent client requests:
1. **`allocateNextDeviceName()`** - Prevents duplicate device ID allocation
2. **`persistLastCommandValue()`** - Atomically updates device state
3. **`persistPermissionsSnapshot()`** - Atomically persists permission changes

### Compilation & Validation
- ✅ All changes compile successfully with `make compile`
- ✅ Server startup smoke test confirms directory structure creation
- ✅ Zero active functional-style code remaining (only imperative loops/switches)
