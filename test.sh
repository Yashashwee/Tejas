g++ tests/hello_world.cpp -o tests/hello_world.o
java -jar jars/tejas.jar src/simulator/config/config.xml tests/run.out tests/hello_world.o
kill -9 $(ps -aux | grep "hello_world.o" | grep -v "grep" | awk '{print $2}')
cat tests/run.out
