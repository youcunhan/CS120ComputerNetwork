import random as r
import sys

res = ''
if len(sys.argv) >= 2:
    count = int(sys.argv[1])
else:
    count = 10000
for i in range(count):
    res += str(r.randint(0,1))


with open("INPUT.txt",'w') as f:
    f.write(res)
