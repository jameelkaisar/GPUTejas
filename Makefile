arch ?= 64
config ?= gputejas/src/simulator/config/config.xml
output ?= output/out.txt


setup:
	@echo "Using arch=$(arch)"
	@echo "pending..."
	$(MAKE) make-jar


make-jar:
	cd gputejas && \
	ant clean && \
	ant && \
	ant make-jar


gen-trace:
	@echo "Using config=$(config)"
ifndef benchmark
	$(error benchmark is not set)
endif
	@echo "Using benchmark=$(benchmark)"
	# $(eval NoOfTPC := $(shell grep -o '<NoOfTPC>.*</NoOfTPC>' $(config) | cut -d'<' -f 2 | cut -d'>' -f 2))
	# $(eval NoOfSM := $(shell grep -o '<NoOfSM>.*</NoOfSM>' $(config) | cut -d'<' -f 2 | cut -d'>' -f 2))
	# $(eval NoOfSP := $(shell grep -o '<NoOfSP>.*</NoOfSP>' $(config) | cut -d'<' -f 2 | cut -d'>' -f 2))
	# $(eval threads := $(shell echo "$(NoOfTPC) * $(NoOfSM) * $(NoOfSP)" | bc))
	$(eval threads := $(shell grep -o '<MaxNumJavaThreads>.*</MaxNumJavaThreads>' $(config) | cut -d'<' -f 2 | cut -d'>' -f 2))
	rm -rf $(threads) 2>/dev/null || true
	g++-4.8 -std=c++0x Tracegen.cpp -c -I .
	g++-4.8 -o tracegen $(benchmark)/*.o Tracegen.o -locelot -ltinfo
	./tracegen $(args) $(threads)
	mkdir $(threads)
	mv *.txt $(threads)
	$(eval kernels := $(shell grep "KERNEL START" $(threads)/0.txt | wc -l))
	java -jar gputejas/jars/Tracesimplifier.jar $(config) tmp . $(kernels)


run:
	@echo "Using config=$(config)"
	@echo "Using output=$(output)"
	# $(eval NoOfTPC := $(shell grep -o '<NoOfTPC>.*</NoOfTPC>' $(config) | cut -d'<' -f 2 | cut -d'>' -f 2))
	# $(eval NoOfSM := $(shell grep -o '<NoOfSM>.*</NoOfSM>' $(config) | cut -d'<' -f 2 | cut -d'>' -f 2))
	# $(eval NoOfSP := $(shell grep -o '<NoOfSP>.*</NoOfSP>' $(config) | cut -d'<' -f 2 | cut -d'>' -f 2))
	# $(eval threads := $(shell echo "$(NoOfTPC) * $(NoOfSM) * $(NoOfSP)" | bc))
	$(eval threads := $(shell grep -o '<MaxNumJavaThreads>.*</MaxNumJavaThreads>' $(config) | cut -d'<' -f 2 | cut -d'>' -f 2))
	$(eval kernels := $(shell ls $(threads)/hashfile_* | wc -l))
	java -jar gputejas/jars/GPUTejas.jar $(config) $(output) . $(kernels)


clean:
	rm -f *.txt *.o tmp tracegen 2>/dev/null || true
