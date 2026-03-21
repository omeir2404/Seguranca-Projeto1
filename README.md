# Seguranca-Projeto1

---

## Username rules:
 - can't contain spaces;
 - can't contain special characters;
 - can't be empty;

## Format of files:

- ### Houses.txt: 
  - `house_id; {Section_id[Device_list]; ...}`
- ### User.txt: 
  - `user_id:password` for each line 
- ### Perms.txt: 
  - `house_id [user_id:section_id,...; user_id:section_id,...]` for each house 1 line with all the permissions for each section

## Duvidas

- se o mesmo utilizador tentar entrar em dois clientes diferentes, ao mesmo tempo, o mesmo utilizador pode ser
  bloqueado? ou desconectar o primeiro?




# 1.Persistence 
This visualization shows how to organize your `server_data` root folder. Using a nested structure for devices makes searching for specific logs much faster.

```text

│
├── server_data/                 # Persistence for the Server 
│   ├── auth/
│   │   ├── users.txt            # <user-id>:<password> 
│   │   └── attestation.txt      # SpertaClient:<size> 
│   ├── metadata/
│   │   ├── homes_registry.txt   # <home>:<owner>:<permissions> 
│   │   └── section_counters.txt # <home>:<section>:<next_id> 
│   ├── states/
│   │   └── last_commands.txt    # <home>:<device>:<value> 
│   └── homes/                   # Directory for device logs 
│       └── Casa1/               # Folder per house 
│           ├── Electros/        # Section folders 
│           ├── Garden/
│           ├── Luzes/           # Contains L1.csv, L2.csv 
│           ├── Multimedia/      # Contains M1.csv, M2.csv 
│           ├── Portas/
│           └── Stores/
│
└── client_data/                 # Local storage for the Client 
    └── <user-id>/               # Separate folders per user (optional but recommended)
        ├── status_Casa1.txt     # Result from RT Casa1 
        └── history_M1_Casa1.csv # Result from RH Casa1 M1 

```

### 2. File Examples & Formats


#### **B. Permissions & Metadata (New Recommended Structure)**
To handle the `ADD` command effectively, you need a way to track who can do what.
* **`homes_registry.txt`**: 
    > `Format -> Home:Owner:User1=Perms;User2=Perms`
    > `Casa1:iberia:alan=M,L` 
    > `Casa2:iberia:alan=all;nuno=G` 

* **`section_counters.txt`**: Required to know the next ID when a user runs `RD <hm> <s>`.
    > `Casa1:M:3` (Next Multimedia device in Casa1 will be M3)
    > `Casa1:L:2`

#### **C. History and State**
* **`last_commands.txt`**: Used for the `RT <hm>` command to quickly show current status without parsing every CSV.
    > `Casa1:M1:45` 
    > `Casa1:L1:30` 

* **`device_logs (CSV)`**: Specifically for the `RH <hm> <d>` command.
    > `Timestamp,Operation,Value` 
    > `2026-03-20 14:00:01,ON,1`
    > `2026-03-20 14:30:00,TIMER,45`


### Implementation Tip for Java
 `Files.createDirectories()` when a user creates a new house or registers a device in a new section. 

