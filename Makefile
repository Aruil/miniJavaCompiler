all: compile

compile:
	javac SpigletGen.java

clean:
	rm -f *.class *~
