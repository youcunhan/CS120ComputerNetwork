import numpy as np
import sounddevice as sd
import time
import matplotlib.pyplot as plt

with open("waves2.txt",'r') as f:
    data = f.read()
waves = data.split('\n')[:-1]
res = []
sin = [0,391,776,1148,1499,1826,2121,2380,2598,2771,2897,2974,3000,2974,2897,2771,2598,2380,2121,1826,1500,1148,776,391,0,-391,-776,-1148,-1499,-1826,-2121,-2380,-2598,-2771,-2897,-2974,-3000,-2974,-2897,-2771,-2598,-2380,-2121,-1826,-1500,-1148,-776,-391]

for i in range(len(waves)):
    wave = waves[i].split(',')[:-1]
    sum = 0
    index = 0
    for j in wave:
        res.append(float(j))
        sum += float(j) * sin[index%48]
        index += 1
    for _ in range(24):
        res.append(0.0)
    res.append(sum)
    for _ in range(23):
        res.append(0.0)
plt.plot(res)
plt.show()