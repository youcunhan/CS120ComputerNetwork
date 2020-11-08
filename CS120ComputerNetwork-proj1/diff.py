with open("OUTPUT.txt",'r') as f:
    data = f.read()
with open("INPUT.txt",'r') as f:
    corr = f.read()

bits = data
corrbits = corr
corrnum = 0
lenth = len(bits)
print("len: "+str(len(bits)))
print("correct len: "+str(len(corrbits)))
for i in range(lenth):
    if corrbits[i] == bits[i]:
        corrnum += 1

print("correct rate: "+str(100*corrnum/lenth)+'%')
