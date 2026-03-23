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
<!-- - [ ] server needs to be restared to recognize new users added to text file -- new issue (Optional according to Professor) -->

## Client CLI and Protocol Loop
- [x] Show command menu after successful authentication.
- [x] Implement command input loop until `CTRL+C`.
- [x] Send each command request to main.server and print reply.
- [x] Validate malformed input and show user-friendly errors.

## Command: CREATE
- [x] Implement `CREATE <hm>` on main.server.
- [x] Return `OK` when home is created.
- [x] Return `NOK` when home already exists.
- [x] Persist home owner and metadata.

## Command: ADD
- [x] Implement `ADD <user1> <hm> <s>`.
- [x] Allow section permission by section letter or full house with `all`.
- [x] Return one of: `OK`, `NOPERM`, `NOHM`, `NOUSER`.
- [x] Persist permission updates.

## Command: RD
- [x] Implement `RD <hm> <s>`.
- [x] Allow only owner to register devices.
- [x] Maintain per-section device counters starting at `1`.
- [x] Generate device IDs from section letter + counter.
- [x] Return one of: `OK`, `NOPERM`, `NOHM`.
- [x] **CRITICAL**: Increment and persist counters in `section_counters.txt` on each successful RD (via `allocateNextDeviceName()`).
- [x] Register device with initial state `0` in `last_commands.txt` (via `persistLastCommandValue()`).

## Command: EC
- [x] Implement `EC <hm> <d> <int>`.
- [x] Validate state/temporization values: `0`, `1`, `2..600`.
- [x] Enforce user permissions before changing device state.
- [x] Return one of: `OK`, `NOK`, `NOHM`, `NOD`, `NOPERM`.
- [x] **CRITICAL**: Persist latest state to `last_commands.txt` after update (via `persistLastCommandValue()`).
- [x] Append timestamped rows to per-device `.csv` history (nested in `server_data/homes/<house>/<section>/<device>.csv`).

## Command: RT
- [x] Implement `RT <hm>`.
- [x] Build data file containing latest command per accessible device (currently sends full logs).
- [x] Use protocol: `OK` + `LONG size` + file bytes.
- [x] Return one of: `NODATA`, `NOHM`, `NOPERM` when invalid.
- [x] Save received file locally on main.client.

## Command: RH
- [x] Implement `RH <hm> <d>`.
- [x] Return device history `.csv` using `OK` + `LONG size` + file bytes.
- [x] Return one of: `NODATA`, `NOHM`, `NOD`, `NOPERM` when invalid.
- [x] Save received file locally on main.client.

## Server Persistence (Spec Required Files)
- [x] **Users.txt** → `server_data/auth/users.txt` - maintains user credentials (User:password).
- [x] **attestation.txt** → `server_data/auth/attestation.txt` - maintains client app hash reference (SpertaClient:<size>).
- [x] **Houses.txt** → `server_data/metadata/homes_registry.txt` - stores house + owner.
- [x] **Permissions.txt** → `server_data/metadata/permissions.txt` - stores user permissions per house.
- [x] **SectionCounters.txt** → `server_data/metadata/section_counters.txt` - Increments counters on RD (format: `<home>:<section>=<next_id>`).
- [x] **DeviceState.txt** → `server_data/states/last_commands.txt` - Persists latest device state/timer values (format: `<home>:<device>:<value>`).
- [x] **Device Logs (CSV)** → `server_data/homes/<house>/<section>/<device>.csv` - Per-device history with timestamp + command (nested structure).



## EXTRAS (Non-spec features)
- [x] LS command - list houses/devices accessible to user (bonus, not in spec). 

## Robustness and Error Handling
- [x] Handle multiple simultaneous TCP clients. 
- [x] Handle `EOFException` and broken sockets cleanly.
- [x] Prevent one failed request from terminating main.client thread.
- [x] Validate integer parsing and command arity safely.
- [x] Cada dispositivo tem um ficheiro de log, no formato csv, o qual contém o seu histórico de funcionamento, ou seja, regista todos os comandos de operação (ligado, ligado com temporização ou desligado) do dispositivo, incluindo a data e hora da sua ocorrência
- [x] same user cant connect twice simultaniously

## Build, Package, and Delivery
- [ ] Build runnable main.client and main.server JARs.
- [ ] Verify compile/run commands are documented.
- [ ] Write `readme.txt` with build steps, run steps, and known limitations.
- [ ] Create `SegC-grupoXXproj1-fase1.zip` with source + JARs + readme.
- [ ] Submit zip in Moodle (one group member).
- [ ] Complete individual contribution self-assessment on Moodle.
- [ ] Final submission before deadline: 27 March, 23:59.


## perguntar:
- [x] Se os logs(e dados a persistir) é para fazer de forma que quermos ou se ha um formato especifico a seguir?
        - [x] RESPOSTA: Os Users e Attestation tem de ser como no enunciado(ja esta correto), o resto é como        acharemos melhores
- [x] se enquanto o servidor tiver a correr, devia reconhecer novos utilizadores adicionados ao ficheiro a mao?
        - [x] RESPOSTA: É como nos queremos, nós temos o server a ler o ficheiro no inicio e a guardar a informacao, depois nao le mais vezes, so escreve em caso de New User


## Bugs e Erros encontrados
- [x] No `CREATE` fazer a estrutura recomendada no enunciado para cada casa -> CADA DISPOSITIVO TEM DE TER UM FICHEIRO CSV
      "Cada dispositivo tem um ficheiro de log, no formato csv, o qual contém o seu histórico de funcionamento, ou seja, regista 
      todos os comandos de operação (ligado, ligado com temporização ou desligado) do dispositivo, incluindo a data e hora da sua ocorrência."
      **FIXED**: Device logs now created in nested structure `server_data/homes/<house>/<section>/<device>.csv` via `getDeviceLogPath()` and `updateDeviceLogs()`.

- [x] `EC` nao esta a funcionar corretamente
- [x] `RT` e `RH` nao guarda os ficheiros no lugar correto
      **FIXED**: RT saves to `client_data/<userId>/status_<house>.txt` | RH saves to `client_data/<userId>/history_<device>_<house>.csv` 
      (via updated `SpertaClient.commandLoop()` with userId parameter).

- [x] Section counters nunca sao atualizadas no ficheiro (so atualisa na memoria entoa se o server parar perde se os dados)
      **FIXED**: Implemented `allocateNextDeviceName()` method (synchronized) that atomically reads counters from `section_counters.txt`, 
      increments per-section counter, and persists immediately on every RD command.

- [x] seccoes novas sao criadas com `RD` (ou nao deixamos que aconteca ou atualizamos os logs para os incluir)
    - [ ] EU (NEXUS) acho que podemos deixar criar, pois cada casa é diferent e pode ser personalizado
- [x] `RD` so ve a primeira letra do input (RD casa1 hahah -> assume seccao = H)
- [x] `RT` é suposto enviar os logs mais recentes por dispositivo mas enviamos TUDO (servercomandhandler -> 458 - 500)
- [ ] `RT` devia enviar `NODATA` se nao houve mudanca -> aberto a interpretacao??
- [x] confirmar que `Attestation` pode ser feito no makefile ou se temos de fazer de outra forma 
      **CONFIRMED**: Attestation is correctly written to `server_data/auth/attestation.txt` via Makefile during compile.
- [x] `Insere aqui o comando: add Eclipse casa pall` Deu `OK` e assumiu Secao == `P`
- [ ] `ls <home>` nao funciona com permissoes == `all`
- [x] Ver se da para as secoes do homes tenham nomes completos e nao so a letra (seccao Garden e nao G)
- [x] ADD esta a criar seccoes novas e npove a primeira letra da seccao como o ponto anterior

## Perguntar à stora
- [x] seccoes novas sao criadas com `RD` (ou nao deixamos que aconteca ou atualizamos os logs para os incluir)
    - [ ] EU (NEXUS) acho que podemos deixar criar, pois cada casa é diferent e pode ser personalizado
- [ ] `RT` devia enviar `NODATA` se nao houve mudanca -> aberto a interpretacao??
- [ ] Se podemos entregar com MakeFile em vez de um script para compilar.