.PHONY: compile run clean

JAVAC = javac
JAVA = java
SRC_DIR = src
BIN_DIR = bin
SERVER_CLASS = main.server.SpertaServer
SERVER_ARGS = 2223
CLIENT_CLASS = main.client.SpertaClient
CLIENT_ARGS = 127.0.0.1:2223 Nexus ehBueFixe
CLIENT_ARGS2 = 127.0.0.1:2223 Eclipse ATuaMae
CLIENT_WRONG_ARGS = 127.0.0.1:2223 Nexus Eclipsew
CLIENT_ARGS_NO_PORT = 127.0.0.1 Nexus ehBueFixe


compile:
	@mkdir -p $(BIN_DIR)/main/server $(BIN_DIR)/main/client
	@$(JAVAC) -d $(BIN_DIR) $(SRC_DIR)/main/server/*.java $(SRC_DIR)/main/client/*.java
	@mkdir -p server_data/auth
	@printf 'SpertaClient:%s\n' "$$(stat -c%s bin/main/client/SpertaClient.class)" > server_data/auth/attestation.txt


compile_client:
	@$(JAVAC) -d $(BIN_DIR)  $(SRC_DIR)/main/client/*.java
	@mkdir -p server_data/auth
	@printf 'SpertaClient:%s\n' "$$(stat -c%s bin/main/client/SpertaClient.class)" > server_data/auth/attestation.txt


compile_server:
	@$(JAVAC) -d $(BIN_DIR) $(SRC_DIR)/main/server/*.java 


# size_check: 
# 	@printf 'SpertaClient:%s\n' "$$(stat -c%s bin/main/client/SpertaClient.class)" > extras/attestation.txt

server: compile
	$(JAVA) -cp $(BIN_DIR) $(SERVER_CLASS) $(SERVER_ARGS)

client: compile
	@$(JAVA) -cp $(BIN_DIR) $(CLIENT_CLASS) $(CLIENT_ARGS)

client2: compile
	@$(JAVA) -cp $(BIN_DIR) $(CLIENT_CLASS) $(CLIENT_ARGS2)

wrongclient: compile
	@$(JAVA) -cp $(BIN_DIR) $(CLIENT_CLASS) $(CLIENT_WRONG_ARGS)

server_no_port: compile
	$(JAVA) -cp $(BIN_DIR) $(SERVER_CLASS) 

client_no_port: compile
	@$(JAVA) -cp $(BIN_DIR) $(CLIENT_CLASS) $(CLIENT_ARGS_NO_PORT)


re: clean compile

clean:
	@rm -rf $(BIN_DIR)
	@rm -rf *.class

cleanFULL: clean
	@rm -rf client_data
	@rm -rf server_data
