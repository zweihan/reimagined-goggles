import csv
import numpy
import matplotlib.pyplot as plt
from sets import Set
from dsp import Signalproc
from ema import ema


f = open("7Traces/6/baro.txt", 'r')

fcsv = csv.reader(f)

gf = open("7Traces/6/GroundTruth.txt", 'r')
gcsv = csv.reader(gf)
for row in gcsv:
	plt.axvline(x=float(row[3]))



ts = list()

baro = list()
baroema = ema(0.1)
baroproc = Signalproc(120)
baro2 = Signalproc(120)
i = 0
for r in fcsv:
	baroproc.update(baroema.avg(float(r[3])))
	i+=1
	if i > 4:
		ts.append(float(r[1]))
		baro.append(baroproc.getStd())

plt.plot(ts, baro, 'r--')
plt.axhline(y=0.5)
plt.show()