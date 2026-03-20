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
<!-- - [ ] server needs to be restared to recognize new users added to text file -- new issue (Optional according to prof) -->

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
- [ ] **CRITICAL**: Increment and persist counters in `SectionCounters.txt` on each successful RD.
- [ ] Register device with initial state `0` in `DeviceState.txt` (latest states file).

## Command: EC
- [x] Implement `EC <hm> <d> <int>`.
- [x] Validate state/temporization values: `0`, `1`, `2..600`.
- [ ] Enforce user permissions before changing device state.
- [x] Return one of: `OK`, `NOK`, `NOHM`, `NOD`, `NOPERM`.
- [ ] **CRITICAL**: Persist latest state to `DeviceState.txt` after update (format: `Casa1:M1=45`).
- [x] Append timestamped rows to per-device `.csv` history.

## Command: RT
- [x] Implement `RT <hm>`.
- [ ] Build data file containing latest command per accessible device (currently sends full logs).
- [x] Use protocol: `OK` + `LONG size` + file bytes.
- [x] Return one of: `NODATA`, `NOHM`, `NOPERM` when invalid.
- [x] Save received file locally on main.client.

## Command: RH
- [x] Implement `RH <hm> <d>`.
- [x] Return device history `.csv` using `OK` + `LONG size` + file bytes.
- [x] Return one of: `NODATA`, `NOHM`, `NOD`, `NOPERM` when invalid.
- [x] Save received file locally on main.client.

## Server Persistence (Spec Required Files)
- [x] **Users.txt** - maintains user credentials (User:password).
- [x] **attestation.txt** - maintains client app hash reference (SpertaClient:<size>).
- [ ] **Houses.txt** - should store house + owner (currently correct).
- [ ] **Permissions.txt** - stores user permissions per house (currently separate file).
- [ ] **SectionCounters.txt** - **CRITICAL**: Must increment counters on RD, not just on CREATE.
- [ ] **DeviceState.txt** - NEW: Must persist latest device state/timer values (Casa1:M1=45; Casa1:M2=0).
- [x] **logs/HOUSE_DEVICE.csv** - per-device history with timestamp + command.

## EXTRAS (Non-spec features)
- [x] LS command - list houses/devices accessible to user (bonus, not in spec). 

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


## perguntar:
- [x] Se os logs(e dados a persistir) é para fazer de forma que quermos ou se ha um formato especifico a seguir?
        - [ ] RESPOSTA: Os Users e Attestation tem de ser como no enunciado(ja esta correto), o resto é como        acharemos melhores
- [x] se enquanto o servidor tiver a correr, devia reconhecer novos utilizadores adicionados ao ficheiro a mao?
        - [ ] RESPOSTA: É como nos queremos, nós temos o server a ler o ficheiro no inicio e a guardar a informacao, depois nao le mais vezes, so escreve em caso de New User


## Bugs e Erros encontrados
- [ ] No `CREATE` fazer a estrutura recomendada no enunciado para cada casa -> CADA DISPOSITIVO TEM DE TER UM FICHEIRO CSV
      "Cada dispositivo tem 
      um ficheiro de log, no formato csv, o qual contém o seu histórico de funcionamento, ou seja, regista 
      todos  os  comandos  de  operação  (ligado,  ligado  com  temporização  ou  desligado)  do  dispositivo, 
      incluindo a data e hora da sua ocorrência."
- [ ] `EC` nao esta a funcionar corretamente
- [ ] `RT` e `RH` nao guarda os ficheiros no lugar correto
- [ ] Section counters nunca sao atualizadas no ficheiro (so atualisa na memoria entoa se o server parar perde se os dados )(servercomandhandler -> 180 - 220)
- [ ] seccoes novas sao criadas com `RD` (ou nao deixamos que aconteca ou atualizamos os logs para os incluir)
- [ ] `RD` so ve a primeira letra do input (RD casa1 hahah -> assume seccao = H)
- [ ] `RT` é suposto enviar os logs mais recentes por dispositivo mas enviamos TUDO (servercomandhandler -> 458 - 500)
- [ ] `RD` e logs tem de ser estruturadas melhor
- [ ] `RT` devia enviar `NODATA` se nao houve mudanca -> aberto a interpretacao??
- [ ] confirmar que `Attestation` pode ser feito no makefile ou se temos de fazer de outra forma 
- [ ] `Insere aqui o comando: add Eclipse casa pall` Deu `OK` e assumiu Secao == `P`
- [ ] `ls <home>` nao funciona com permissoes == `all`


## LOG structure

### 1.Persistence 
This visualization shows how to organize your `server_data` root folder. Using a nested structure for devices makes searching for specific logs much faster.

```text
server_data/
├── auth/
│   ├── users.txt                # <user-id>:<password>  
│   └── attestation.txt          # <app-name>:<size>  
├── metadata/
│   ├── homes_registry.txt       # <home>:<owner>:<permissions> 
│   └── section_counters.txt     # <home>:<section>:<next_id> 
├── states/
│   └── last_commands.txt        # <home>:<device>:<value>
└── homes/                       # Root for houses 
    ├── Casa1/
    │   ├── Electros/
    │   ├── Garden/
    │   ├── Luzes/               # Contains L1.csv, L2.csv, etc. 
    │   ├── Multimedia/          # Contains M1.csv, M2.csv 
    │   ├── Portas/              # Contains P1.csv, P2.csv 
    │   └── Stores/
    ├── Casa2/
    │   └── ... (same subfolders)
    └── Casa3/
        └── ... (same subfolders)

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

