.PHONY: compile run clean

JAVAC = javac
JAVA = java
SRC_DIR = src
BIN_DIR = bin
SERVER_CLASS = SpertaServer
SERVER_ARGS = 2223
CLIENT_CLASS = SpertaClient
CLIENT_ARGS = 127.0.0.1:2223 Nexus Eclipse
CLIENT_WRONG_ARGS = 127.0.0.1:2223 Nexus Eclipsew


compile:
	@$(JAVAC) -d $(BIN_DIR) $(SRC_DIR)/*.java

server: compile
	@$(JAVA) -cp $(BIN_DIR) $(SERVER_CLASS) $(SERVER_ARGS)

client: compile
	@$(JAVA) -cp $(BIN_DIR) $(CLIENT_CLASS) $(CLIENT_ARGS)

wrongclient: compile
	@$(JAVA) -cp $(BIN_DIR) $(CLIENT_CLASS) $(CLIENT_WRONG_ARGS)

re: clean compile

clean:
	@rm -rf $(BIN_DIR)
	@rm -rf *.class