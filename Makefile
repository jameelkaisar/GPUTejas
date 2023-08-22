arch ?= 64
config ?= gputejas/src/simulator/config/config.xml
output ?= output/out.txt


setup:
	@echo "Using arch=$(arch)"

	sudo apt update
	sudo apt install -y software-properties-common build-essential wget unzip

	sudo add-apt-repository 'deb http://archive.ubuntu.com/ubuntu/ trusty universe'
	sudo add-apt-repository 'deb http://archive.ubuntu.com/ubuntu/ trusty main'
	sudo apt-key adv --keyserver keyserver.ubuntu.com --recv-keys 40976EAF437D05B5
	sudo apt-key adv --keyserver keyserver.ubuntu.com --recv-keys 3B4FE6ACC0B21F32
	sudo apt update
	sudo apt install -y gcc-4.4 gcc-4.8
	sudo apt install -y g++-4.4 g++-4.8

	mkdir tmp || true

# cp files/jdk-7u80-linux-x64.tar.gz tmp/jdk-7u80-linux-x64.tar.gz
	wget -O tmp/jdk-7u80-linux-x64.tar.gz https://repo.huaweicloud.com/java/jdk/7u80-b15/jdk-7u80-linux-x64.tar.gz
	sudo mkdir -p /usr/lib/jvm
	sudo tar -xf tmp/jdk-7u80-linux-x64.tar.gz -C /usr/lib/jvm
	sudo ln -s /usr/lib/jvm/jdk1.7.0_80 /usr/lib/jvm/java-7-oracle

# cp files/apache-ant-1.9.15-bin.zip tmp
	wget -O tmp/apache-ant-1.9.15-bin.zip https://archive.apache.org/dist/ant/binaries/apache-ant-1.9.15-bin.zip
	sudo unzip -d /opt tmp/apache-ant-1.9.15-bin.zip

# cp files/nvidia-cuda-toolkit_4.0.17.orig.tar.gz tmp
	wget -O tmp/nvidia-cuda-toolkit_4.0.17.orig.tar.gz https://launchpad.net/ubuntu/+archive/primary/+sourcefiles/nvidia-cuda-toolkit/4.0.17-3/nvidia-cuda-toolkit_4.0.17.orig.tar.gz
	tar -xf tmp/nvidia-cuda-toolkit_4.0.17.orig.tar.gz -C tmp
	sudo tmp/nvidia-cuda-toolkit-4.0.17.orig/cudatoolkit_4.0.17_linux_64_ubuntu10.10.run --noexec  --target /usr/local/cuda

# sudo update-alternatives --install /usr/bin/gcc gcc `readlink -f $(shell which gcc)` 99 --slave /usr/bin/g++ g++ `readlink -f $(shell which g++)`
	sudo update-alternatives --install /usr/bin/gcc gcc /usr/bin/gcc-4.4 44 --slave /usr/bin/g++ g++ /usr/bin/g++-4.4
	sudo update-alternatives --install /usr/bin/gcc gcc /usr/bin/gcc-4.8 48 --slave /usr/bin/g++ g++ /usr/bin/g++-4.8
	sudo update-alternatives --set gcc /usr/bin/gcc-4.4
#33 1.204 /usr/include/x86_64-linux-gnu/bits/floatn.h(74): error: invalid argument to attribute "__mode__"
#33 1.204 
#33 1.204 /usr/include/x86_64-linux-gnu/bits/floatn.h(86): error: identifier "__float128" is undefined
# Add the following lines to floatn.h
# +#if CUDART_VERSION
# +#undef __HAVE_FLOAT128
# +#define __HAVE_FLOAT128 0
# +#endif
	sudo cp files/floatn.h /usr/include/x86_64-linux-gnu/bits/floatn.h

ifeq ($(arch),32)
	sudo cp so_files_32bit/libocelot.so /usr/lib/libocelot.so
# sudo rm /usr/lib/i386-linux-gnu/libtinfo.so 2>/dev/null || true
	sudo cp so_files_32bit/libtinfo.so /usr/lib/i386-linux-gnu/libtinfo.so
	sudo cp so_files_32bit/libboost_thread.so.1.54.0 '/usr/lib/i386-linux-gnu/libboost_thread.so.1.54.0'
	sudo cp so_files_32bit/libboost_system.so.1.54.0 '/usr/lib/i386-linux-gnu/libboost_system.so.1.54.0'
	sudo cp so_files_32bit/libz.so.1.2.8 /lib/i386-linux-gnu/libz.so
# sudo rm /usr/lib/i386-linux-gnu/libGLEW.so.1.10 2>/dev/null || true
	sudo cp so_files_32bit/libGLEW.so.1.10 /usr/lib/i386-linux-gnu/libGLEW.so.1.10
	sudo apt install -y libgl1-mesa-glx
else ifeq ($(arch),64)
	sudo cp so_files_64bit/libocelot.so /usr/lib/libocelot.so
# sudo rm /usr/lib/x86_64-linux-gnu/libtinfo.so 2>/dev/null || true
	sudo cp so_files_64bit/libtinfo.so /usr/lib/libtinfo.so
	sudo cp so_files_64bit/libboost_thread.so.1.54.0 /usr/lib/libboost_thread.so.1.54.0
	sudo cp so_files_64bit/libboost_system.so.1.54.0 /usr/lib/libboost_system.so.1.54.0
	sudo cp so_files_64bit/libz.so.1.2.8 /lib/libz.so
# sudo rm /usr/lib/x86_64-linux-gnu/libGLEW.so.1.10 2>/dev/null || true
	sudo cp so_files_64bit/libGLEW.so.1.10 /usr/lib/libGLEW.so.1.10
	sudo apt install -y libgl1-mesa-glx
else
	$(error arch not supported)
endif

	rm -rf tmp/*

	@echo ''
	@echo 'Please run the following commands to complete the setup:'
	@echo '> export JAVA_HOME="/usr/lib/jvm/java-7-oracle"'
	@echo '> export PATH="$$PATH:$$JAVA_HOME/bin"'
	@echo '> export ANT_HOME="/opt/apache-ant-1.9.15"'
	@echo '> export PATH="$$PATH:$$ANT_HOME/bin"'
	@echo '> export PATH="$$PATH:/usr/local/cuda/bin"'
	@echo '> export LD_LIBRARY_PATH="$$LD_LIBRARY_PATH:/usr/local/cuda/lib64:/usr/local/cuda/lib"'
	@echo '> make make-jar'


make-jar:
	cd gputejas && \
	ant clean && \
	ant && \
	ant make-jar && \
	ant make-trace-jar


gen-trace:
	@echo "Using config=$(config)"
ifndef benchmark
	$(error benchmark is not set)
endif
	@echo "Using benchmark=$(benchmark)"
	$(eval NoOfTPC := $(shell grep -o '<NoOfTPC>.*</NoOfTPC>' $(config) | cut -d'<' -f 2 | cut -d'>' -f 2))
	$(eval NoOfSM := $(shell grep -o '<NoOfSM>.*</NoOfSM>' $(config) | cut -d'<' -f 2 | cut -d'>' -f 2))
	$(eval NoOfSP := $(shell grep -o '<NoOfSP>.*</NoOfSP>' $(config) | cut -d'<' -f 2 | cut -d'>' -f 2))
	$(eval threads := $(shell echo "$(NoOfTPC) * $(NoOfSM) * $(NoOfSP)" | bc))
# $(eval threads := $(shell grep -o '<MaxNumJavaThreads>.*</MaxNumJavaThreads>' $(config) | cut -d'<' -f 2 | cut -d'>' -f 2))
	rm -rf $(threads) 2>/dev/null || true
	g++-4.8 -std=c++0x Tracegen.cpp -c -I .
	g++-4.8 -o tracegen $(benchmark)/*.o Tracegen.o -locelot -ltinfo
	./tracegen $(args) $(threads)
	mkdir $(threads) || true
	mv *.txt $(threads)


gen-sim:
	@echo "Using config=$(config)"
	$(eval NoOfTPC := $(shell grep -o '<NoOfTPC>.*</NoOfTPC>' $(config) | cut -d'<' -f 2 | cut -d'>' -f 2))
	$(eval NoOfSM := $(shell grep -o '<NoOfSM>.*</NoOfSM>' $(config) | cut -d'<' -f 2 | cut -d'>' -f 2))
	$(eval NoOfSP := $(shell grep -o '<NoOfSP>.*</NoOfSP>' $(config) | cut -d'<' -f 2 | cut -d'>' -f 2))
	$(eval threads := $(shell echo "$(NoOfTPC) * $(NoOfSM) * $(NoOfSP)" | bc))
# $(eval threads := $(shell grep -o '<MaxNumJavaThreads>.*</MaxNumJavaThreads>' $(config) | cut -d'<' -f 2 | cut -d'>' -f 2))
	echo $(eval kernels := $(shell grep "KERNEL START" $(threads)/0.txt | wc -l))
	java -jar gputejas/jars/Tracesimplifier.jar $(config) tmp . $(kernels)


run:
	@echo "Using config=$(config)"
	@echo "Using output=$(output)"
	$(eval NoOfTPC := $(shell grep -o '<NoOfTPC>.*</NoOfTPC>' $(config) | cut -d'<' -f 2 | cut -d'>' -f 2))
	$(eval NoOfSM := $(shell grep -o '<NoOfSM>.*</NoOfSM>' $(config) | cut -d'<' -f 2 | cut -d'>' -f 2))
	$(eval NoOfSP := $(shell grep -o '<NoOfSP>.*</NoOfSP>' $(config) | cut -d'<' -f 2 | cut -d'>' -f 2))
	$(eval threads := $(shell echo "$(NoOfTPC) * $(NoOfSM) * $(NoOfSP)" | bc))
# $(eval threads := $(shell grep -o '<MaxNumJavaThreads>.*</MaxNumJavaThreads>' $(config) | cut -d'<' -f 2 | cut -d'>' -f 2))
	$(eval kernels := $(shell ls $(threads)/hashfile_* | wc -l))
	java -jar gputejas/jars/GPUTejas.jar $(config) $(output) . $(kernels)


clean:
	rm -f *.txt *.o tmp tracegen 2>/dev/null || true
