compile:
	./mvnw -q compile

run:
	./mvnw -q javafx:run

clean:
	./mvnw -q clean

rebuild: clean compile
