with open("INPUT1to2.bin",'rb') as f:
    corrbits = f.read()
with open("OUTPUT.bin",'rb') as f:
    bits = f.read()

corrnum = 0
lenth = len(bits)
print("len: "+str(len(bits)))
print("correct len: "+str(len(corrbits)))
for i in range(lenth):
    if corrbits[i] == bits[i]:
        corrnum += 1
    # else:
    #     print("wrong:", i, bits[i], corrbits[i])

print("correct rate: "+str(100*corrnum/lenth)+'%')
