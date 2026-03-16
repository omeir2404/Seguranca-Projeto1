# SEG2526 Projeto 1 (Fase 1) - TODO

## Core Setup
- [x] Validate startup args for `SpertaServer [port]` (default `22345`).
- [x] Validate startup args for `SpertaClient <serverAddress> <user-id> <password>`.
- [x] Support optional `:port` in `serverAddress` (default `22345`).

## Networking and Concurrency
- [x] Keep main.server listening for multiple simultaneous TCP clients.
- [x] Handle each main.client in a dedicated thread.
- [x] Ensure graceful disconnect handling without crashing main.server.

## Attestation and Authentication
- [x] Implement main.client attestation before authentication.
- [x] Client sends `SpertaClient` size metadata.
- [x] Server validates against local reference file (`SpertaClient:<size>`).
- [x] Return attestation result and print exactly `ATTESTATION OK` / `ATTESTATION FAILED` on main.client.
- [x] Implement auth outcomes: `WRONG-PWD`, `OK-NEW-USER`, `OK-USER`.
- [x] On `WRONG-PWD`, allow main.client password retry.
- [x] Persist users and passwords on main.server text file.
- [ ] server needs to be restared to recognize new users added to text file -- new issue

## Client CLI and Protocol Loop
- [x] Show command menu after successful authentication.
- [x] Implement command input loop until `CTRL+C`.
- [ ] Send each command request to main.server and print reply.
- [ ] Validate malformed input and show user-friendly errors.

## Command: CREATE
- [x] Implement `CREATE <hm>` on main.server.
- [x] Return `OK` when home is created.
- [x] Return `NOK` when home already exists.
- [ ] Persist home owner and metadata.

## Command: ADD
- [x] Implement `ADD <user1> <hm> <s>`.
- [x] Allow section permission by section letter or full house with `all`.
- [x] Return one of: `OK`, `NOPERM`, `NOHM`, `NOUSER`.
- [ ] Persist permission updates.

## Command: RD
- [x] Implement `RD <hm> <s>`.
- [x] Allow only owner to register devices.
- [x] Maintain per-section device counters starting at `1`.
- [x] Generate device IDs from section letter + counter.
- [x] Return one of: `OK`, `NOPERM`, `NOHM`.
- [ ] Persist counters and devices.

## Command: EC
- [x] Implement `EC <hm> <d> <int>`.
- [x] ValsendIntidate state/temporization values: `0`, `1`, `2..600`.
- [ ] Enforce user permissions before changing device state.
- [x] Return one of: `OK`, `NOK`, `NOHM`, `NOD`, `NOPERM`.
- [ ] Persist latest state per device.
- [x] Append timestamped rows to per-device `.csv` history.

## Command: RT
- [ ] Implement `RT <hm>`.
- [ ] Build data file containing latest command per accessible device.
- [ ] Use protocol: `OK` + `LONG size` + file bytes.
- [ ] Return one of: `NODATA`, `NOHM`, `NOPERM` when invalid.
- [ ] Save received file locally on main.client.

## Command: RH
- [ ] Implement `RH <hm> <d>`.
- [ ] Return device history `.csv` using `OK` + `LONG size` + file bytes.
- [ ] Return one of: `NODATA`, `NOHM`, `NOD`, `NOPERM` when invalid.
- [ ] Save received file locally on main.client.

## Server Persistence
- [x] Maintain users/passwords file.
- [ ] Maintain homes/devices/permissions file.
- [ ] Maintain latest state/temporization file.
- [ ] Maintain per-device `.csv` logs.
- [ ] Maintain section-counter file(s).

## EXTRAS
- [ ] Show houses comand -"ls"
- [ ] 

## Robustness and Error Handling
- [ ] Handle multiple simultaneous TCP clients. 
- [ ] Handle `EOFException` and broken sockets cleanly.
- [ ] Prevent one failed request from terminating main.client thread.
- [ ] Validate integer parsing and command arity safely.
- [ ] Cada dispositivo tem um ficheiro de log, no formato csv, o qual contém o seu histórico de funcionamento, ou seja, regista todos os comandos de operação (ligado, ligado com temporização ou desligado) do dispositivo, incluindo a data e hora da sua ocorrência

## Build, Package, and Delivery
- [ ] Build runnable main.client and main.server JARs.
- [ ] Verify compile/run commands are documented.
- [ ] Write `readme.txt` with build steps, run steps, and known limitations.
- [ ] Create `SegC-grupoXXproj1-fase1.zip` with source + JARs + readme.
- [ ] Submit zip in Moodle (one group member).
- [ ] Complete individual contribution self-assessment on Moodle.
- [ ] Final submission before deadline: 27 March, 23:59.
