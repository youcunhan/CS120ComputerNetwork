import sounddevice as sd
import soundfile as sf
import time
import threading

class myThread (threading.Thread):
    def __init__(self, threadID, name, counter):
        threading.Thread.__init__(self)
        self.threadID = threadID
        self.name = name
        self.counter = counter
    def run(self):
        data, fs = sf.read("sample_track.wav", dtype='float32')
        sd.play(data, fs)
        
def CK1():
    time.sleep(1)
    duration = 10  # seconds
    sampleRate = 48000
    print("start recording")
    myrecording = sd.rec(int(duration * sampleRate), samplerate=sampleRate, channels=1)
    for i in range(10):
        print(str(10-i)+" seconds left")
        time.sleep(1)
    sd.wait()
    print("finish recording")
    sd.play(myrecording)
    print("start playing")
    sd.wait()
    print("finish playing")
def CK2():
    duration = 10  # seconds
    sampleRate = 48000
    time.sleep(1)
    print("start recording")
    playthread = myThread(1, "Thread-1", 1)
    playthread.start()
    myrecording = sd.rec(int(duration * sampleRate), samplerate=sampleRate, channels=1)
    for i in range(10):
        print(str(10-i)+" seconds left")
        time.sleep(1)
    print("finish recording")
    playthread.join()
    sd.play(myrecording)
    print("start playing")
    sd.wait()
    print("finish playing")

CK2()