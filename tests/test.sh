g++ -o hello.o -static hello_world.cpp

java -jar /home/sir_killsalot/Downloads/MTECH/TejasTest/Tejas_1.4.1/Tejas/jars/tejas.jar /home/sir_killsalot/Downloads/MTECH/TejasTest/Tejas_1.4.1/Tejas/src/simulator/config/config.xml /home/sir_killsalot/Downloads/MTECH/TejasTest/Tejas_1.4.1/Tejas/tests/hello.out /home/sir_killsalot/Downloads/MTECH/TejasTest/Tejas_1.4.1/Tejas/tests/hello.o

kill -9 $(ps -aux | grep "hello.o" | grep -v "grep" | awk '{print $2}')

