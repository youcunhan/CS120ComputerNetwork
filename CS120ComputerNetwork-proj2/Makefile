all:
	javac Node1.java Node2.java Transmitter.java Receiver.java Util.java
T:
	javac Node2.java Transmitter.java Receiver.java Util.java
	java Node2 Transmit
	python diff.py
macperf:
	javac Node1.java Transmitter.java Receiver.java Util.java
	java Node1 macperf
macping:
	javac Node1.java Transmitter.java Receiver.java Util.java
	java Node1 macping
macping_receive:
	javac Node2.java Transmitter.java Receiver.java Util.java
	java Node2 macping_receive
decode:
	javac Node1.java Transmitter.java Receiver.java Util.java
	java Node1 decode
	python diff.py
findmax:
	javac Node2.java Transmitter.java Receiver.java Util.java
	java Node2 findmax
testall:
	javac Node2.java Transmitter.java Receiver.java Util.java
	java Node2 testall
Util:
	javac Util.java
	java Util
git:
	git add .
	git commit -m "trival submit"
	git push