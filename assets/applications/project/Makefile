
build:
	javac -d out/ -cp ./lib/ojdbc8.jar cs174a/*.java

clean:
	rm ./out/cs174a/*

run:
	java -cp ./lib/ojdbc8.jar:out:. cs174a.Main

#Need to re build to see changes
gui:
	make build
	java -classpath ./out:./lib/* cs174a.Main
