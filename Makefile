.PHONY: compile run clean

JAVAC = javac
JAVA = java
SRC_DIR = src
BIN_DIR = bin
SERVER_CLASS = SpertaServer
CLIENT_CLASS = SpertaClient

compile:
	$(JAVAC) -d $(BIN_DIR) $(SRC_DIR)/*.java

server: compile
	$(JAVA) -cp $(BIN_DIR) $(SERVER_CLASS)

client: compile
	$(JAVA) -cp $(BIN_DIR) $(CLIENT_CLASS)

re: clean compile

clean:
	rm -rf $(BIN_DIR)
	rm -rf *.class