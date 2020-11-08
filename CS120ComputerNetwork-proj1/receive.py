import numpy as np
import sounddevice as sd
import soundfile as sf
import matplotlib.pyplot as plt
import time


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


		
print("start receive")
time.sleep(1)
duration = 5  # seconds
sampleRate = 48000
print("start receive")
myrecording = sd.rec(int(duration * sampleRate), samplerate=sampleRate, channels=1)
sd.wait()
print("end receive")

##deal with your sounds
plt.plot(myrecording)
plt.title("received signal")
plt.show()

state = 0 ##代表正在等待包头

for sample in myrecording:
##deal with your sounds
#先找包头
	if state == 0:
		读sample进来
		if(找到包头):
			state = 1
	if state == 1:
		读sample进来
		if(达到单个symbol的长度， == samples_per_symbol):
			解包
			if(symbol达到单个package的长度):
				读完全部数据就退出
				没读完就state = 0