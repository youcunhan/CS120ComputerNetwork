import numpy as np
import sounddevice as sd
import time
import matplotlib.pyplot as plt

with open("waves.txt",'r') as f:
    data = f.read()
waves = data.split(',')[:-1]
for i in range(len(waves)):
    waves[i] = float(waves[i])
plt.plot(waves)
plt.show()