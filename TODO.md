# SEG2526 Projeto 1 (Fase 1) - TODO

## Core Setup
- [x] Validate startup args for `SpertaServer [port]` (default `22345`).
- [ ] Validate startup args for `SpertaClient <serverAddress> <user-id> <password>`.
- [ ] Support optional `:port` in `serverAddress` (default `22345`).

## Networking and Concurrency
- [ ] Keep server listening for multiple simultaneous TCP clients.
- [ ] Handle each client in a dedicated thread.
- [ ] Ensure graceful disconnect handling without crashing server.

## Attestation and Authentication
- [ ] Implement client attestation before authentication.
- [ ] Client sends `SpertaClient` size metadata.
- [ ] Server validates against local reference file (`SpertaClient:<size>`).
- [ ] Return attestation result and print exactly `ATTESTATION OK` / `ATTESTATION FAILED` on client.
- [ ] Implement auth outcomes: `WRONG-PWD`, `OK-NEW-USER`, `OK-USER`.
- [ ] On `WRONG-PWD`, allow client password retry.
- [ ] Persist users and passwords on server text file.

## Client CLI and Protocol Loop
- [ ] Show command menu after successful authentication.
- [ ] Implement command input loop until `CTRL+C`.
- [ ] Send each command request to server and print reply.
- [ ] Validate malformed input and show user-friendly errors.

## Command: CREATE
- [ ] Implement `CREATE <hm>` on server.
- [ ] Return `OK` when home is created.
- [ ] Return `NOK` when home already exists.
- [ ] Persist home owner and metadata.

## Command: ADD
- [ ] Implement `ADD <user1> <hm> <s>`.
- [ ] Allow section permission by section letter or full house with `all`.
- [ ] Return one of: `OK`, `NOPERM`, `NOHM`, `NOUSER`.
- [ ] Persist permission updates.

## Command: RD
- [ ] Implement `RD <hm> <s>`.
- [ ] Allow only owner to register devices.
- [ ] Maintain per-section device counters starting at `1`.
- [ ] Generate device IDs from section letter + counter.
- [ ] Return one of: `OK`, `NOPERM`, `NOHM`.
- [ ] Persist counters and devices.

## Command: EC
- [ ] Implement `EC <hm> <d> <int>`.
- [ ] Validate state/temporization values: `0`, `1`, `2..600`.
- [ ] Enforce user permissions before changing device state.
- [ ] Return one of: `OK`, `NOK`, `NOHM`, `NOD`, `NOPERM`.
- [ ] Persist latest state per device.
- [ ] Append timestamped rows to per-device `.csv` history.

## Command: RT
- [ ] Implement `RT <hm>`.
- [ ] Build data file containing latest command per accessible device.
- [ ] Use protocol: `OK` + `LONG size` + file bytes.
- [ ] Return one of: `NODATA`, `NOHM`, `NOPERM` when invalid.
- [ ] Save received file locally on client.

## Command: RH
- [ ] Implement `RH <hm> <d>`.
- [ ] Return device history `.csv` using `OK` + `LONG size` + file bytes.
- [ ] Return one of: `NODATA`, `NOHM`, `NOD`, `NOPERM` when invalid.
- [ ] Save received file locally on client.

## Server Persistence
- [ ] Maintain users/passwords file.
- [ ] Maintain homes/devices/permissions file.
- [ ] Maintain latest state/temporization file.
- [ ] Maintain per-device `.csv` logs.
- [ ] Maintain section-counter file(s).

## Robustness and Error Handling
- [ ] Handle `EOFException` and broken sockets cleanly.
- [ ] Prevent one failed request from terminating client thread.
- [ ] Validate integer parsing and command arity safely.

## Build, Package, and Delivery
- [ ] Build runnable client and server JARs.
- [ ] Verify compile/run commands are documented.
- [ ] Write `readme.txt` with build steps, run steps, and known limitations.
- [ ] Create `SegC-grupoXXproj1-fase1.zip` with source + JARs + readme.
- [ ] Submit zip in Moodle (one group member).
- [ ] Complete individual contribution self-assessment on Moodle.
- [ ] Final submission before deadline: 27 March, 23:59.
