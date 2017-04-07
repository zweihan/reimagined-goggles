import numpy as np

class Signalproc:
	def __init__(self, windowSize):
		self.windowSize = windowSize
		self.sumofabs = 0.0
		self.sum = 0.0
		self.threshold = 10
		self.thresholdCount = 0
		self.sumofsquares = 0.0
		self.val = np.zeros(windowSize)
		self.nextValIndex = 0
		self.processedVal = 0


	def update(self, newVal):
		oldVal = self.val[self.nextValIndex]
		if oldVal > self.threshold:
			self.thresholdCount -= 1
		if newVal > self.threshold:
			self.thresholdCount += 1
		self.sumofabs = self.sumofabs + abs(newVal) - abs(oldVal)
		self.sum = self.sum + newVal - oldVal
		self.sumofsquares = self.sumofsquares + newVal * newVal - oldVal * oldVal
		self.val[self.nextValIndex] = newVal
		if self.nextValIndex >= self.windowSize - 1:
			self.nextValIndex = 0
		else:
			self.nextValIndex += 1
		self.processedVal+=1
	def getSS(self):
		return self.sumofsquares/self.windowSize
	def getThresholdCount(self):
		return self.thresholdCount
	def findMax(self):
		max = 0
		for v in self.val:
			if abs(v) > max:
				max = abs(v)
		return max

	def getMeanOfAbs(self):
		return self.sumofabs/self.windowSize

	def getStdOfAbs(self):
		return np.sqrt(self.sumofsquares / self.windowSize - (self.sumofabs / self.windowSize * self.sumofabs / self.windowSize))

	def getMean(self):
		return self.sum / self.windowSize

	def getStd(self):
		if self.windowSize < self.processedVal:
			count = self.windowSize
		else:
			count = self.processedVal
		return np.sqrt(self.sumofsquares/count - (self.sum/count * self.sum/count))


