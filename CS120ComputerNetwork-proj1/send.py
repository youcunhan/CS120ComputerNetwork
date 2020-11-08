import numpy as np
import sounddevice as sd
import time
import matplotlib.pyplot as plt

samplerate = 48000

##design your own carrier for 0 and 1 here --crtql
samples_per_symbol = 48
t = np.arange(samples_per_symbol)/samplerate
carrier0 = (-1)*np.sin(2 * np.pi * 1000 * t)
carrier1 = np.sin(2 * np.pi * 1000 * t)

##design your own preamble here --crtql
samples_for_preamble = 480
t = np.arange(samples_for_preamble)/samplerate
preamble = np.sin(2 * np.pi * 7000 * t)

##数据量 --陈蓉太强了
datalen = 10000

##发几个包 --陈蓉太强了
package_num = 100

##每个包多少数据 --陈蓉太强了
symbols_per_package = (int)(datalen/package_num)

print("reading data, please wait!")
waves = np.zeros(0)
with open("INPUT.txt") as f:
    bits = f.read()

i = 0
for bit in bits:
    if i % symbols_per_package == 0:
        #发包头 --陈蓉太强了
        waves = np.concatenate([waves,np.array(preamble)])
    if bit == '0':
        waves = np.concatenate([waves,np.array(carrier0)])
    elif bit == '1':
        waves = np.concatenate([waves,np.array(carrier1)])
    i += 1

##start send --crtql
print("start send after 1 seconds")
time.sleep(1)
sd.play(waves)
sd.wait()