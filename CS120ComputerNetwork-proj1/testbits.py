import numpy as np
import sounddevice as sd
import time
import matplotlib.pyplot as plt

with open("waves2.txt",'r') as f:
    data = f.read()
sins = data.split('\n')[0].split(',')[:-1]
waves = data.split('\n')[1].split(',')[:-1]
for i in range(len(waves)):
    waves[i] = float(waves[i])
    sins[i] = float(sins[i])
plt.plot(waves)
plt.plot(sins)
plt.show()