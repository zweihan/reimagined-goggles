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

light = list()
lightema = ema(0.1)
lightproc = Signalproc(120)
baro2 = Signalproc(120)
i = 0
for r in fcsv:
	i+=1
	if i > 4:
		ts.append(float(r[1]))
		light.append(float(r[2]))

plt.plot(ts, light, 'r--')
plt.axhline(y=1500)
plt.show()