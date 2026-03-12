.PHONY: compile run clean

JAVAC = javac
JAVA = java
SRC_DIR = src
BIN_DIR = bin
SERVER_CLASS = main.server.SpertaServer
SERVER_ARGS = 2223
CLIENT_CLASS = main.client.SpertaClient
CLIENT_ARGS = 127.0.0.1:2223 Nexus ehBueFixe
CLIENT_WRONG_ARGS = 127.0.0.1:2223 Nexus Eclipsew


compile:
	@mkdir -p $(BIN_DIR)/main/server $(BIN_DIR)/main/client
	@$(JAVAC) -d $(BIN_DIR) $(SRC_DIR)/main/server/*.java $(SRC_DIR)/main/client/*.java

server: compile
	$(JAVA) -cp $(BIN_DIR) $(SERVER_CLASS) $(SERVER_ARGS)

client: compile
	@$(JAVA) -cp $(BIN_DIR) $(CLIENT_CLASS) $(CLIENT_ARGS)

wrongclient: compile
	@$(JAVA) -cp $(BIN_DIR) $(CLIENT_CLASS) $(CLIENT_WRONG_ARGS)

re: clean compile

clean:
	@rm -rf $(BIN_DIR)
	@rm -rf *.class