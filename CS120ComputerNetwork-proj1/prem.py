import numpy as np
import sounddevice as sd
import time
import matplotlib.pyplot as plt

with open("waves.txt",'r') as f:
    data = f.read()
mywaves = data.split('\n')[0].split(',')[:-1]
waves = data.split('\n')[1].split(',')[:-1]
restbits = data.split('\n')[2].split(',')[:-1]
for i in range(len(waves)):
    waves[i] = float(waves[i])
for i in range(len(mywaves)):
    mywaves[i] = float(mywaves[i])
for i in range(len(restbits)):
    restbits[i] = float(restbits[i])
plt.plot(mywaves)
plt.title("mywave")
plt.figure()
plt.plot(waves)
plt.title("samplewave")
plt.figure()
plt.plot(restbits)
plt.title("restbits")
plt.show()