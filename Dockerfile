FROM ubuntu:latest

WORKDIR /home

RUN apt update
RUN apt install -y software-properties-common build-essential wget unzip

RUN add-apt-repository 'deb http://archive.ubuntu.com/ubuntu/ trusty universe'
RUN add-apt-repository 'deb http://archive.ubuntu.com/ubuntu/ trusty main'
RUN apt-key adv --keyserver keyserver.ubuntu.com --recv-keys 40976EAF437D05B5
RUN apt-key adv --keyserver keyserver.ubuntu.com --recv-keys 3B4FE6ACC0B21F32
RUN apt update
RUN apt install -y gcc-4.4 gcc-4.8
RUN apt install -y g++-4.4 g++-4.8

RUN mkdir tmp

# COPY files/jdk-7u80-linux-x64.tar.gz tmp/jdk-7u80-linux-x64.tar.gz
RUN wget -O tmp/jdk-7u80-linux-x64.tar.gz https://repo.huaweicloud.com/java/jdk/7u80-b15/jdk-7u80-linux-x64.tar.gz
RUN mkdir -p /usr/lib/jvm
RUN tar -xf tmp/jdk-7u80-linux-x64.tar.gz -C /usr/lib/jvm
RUN ln -s /usr/lib/jvm/jdk1.7.0_80 /usr/lib/jvm/java-7-oracle
ENV JAVA_HOME /usr/lib/jvm/java-7-oracle
ENV PATH $PATH:$JAVA_HOME/bin

# COPY files/apache-ant-1.9.15-bin.zip tmp
RUN wget -O tmp/apache-ant-1.9.15-bin.zip https://archive.apache.org/dist/ant/binaries/apache-ant-1.9.15-bin.zip
RUN unzip -d /opt tmp/apache-ant-1.9.15-bin.zip
ENV ANT_HOME /opt/apache-ant-1.9.15
ENV PATH $PATH:$ANT_HOME/bin

# COPY files/nvidia-cuda-toolkit_4.0.17.orig.tar.gz tmp
RUN wget -O tmp/nvidia-cuda-toolkit_4.0.17.orig.tar.gz https://launchpad.net/ubuntu/+archive/primary/+sourcefiles/nvidia-cuda-toolkit/4.0.17-3/nvidia-cuda-toolkit_4.0.17.orig.tar.gz
RUN tar -xf tmp/nvidia-cuda-toolkit_4.0.17.orig.tar.gz -C tmp
RUN tmp/nvidia-cuda-toolkit-4.0.17.orig/cudatoolkit_4.0.17_linux_64_ubuntu10.10.run --noexec  --target /usr/local/cuda
ENV PATH="/usr/local/cuda/bin:${PATH}"
ENV LD_LIBRARY_PATH="/usr/local/cuda/lib64:/usr/local/cuda/lib:${LD_LIBRARY_PATH}"

# COPY files/gputejas.zip tmp
# RUN unzip -d . tmp/gputejas.zip
# RUN wget -O tmp/gputejas_installation_kit.tar.gz https://www.cse.iitd.ac.in/tejas/gputejas/home_files/gputejas_installation_kit.tar.gz
# RUN tar -xf tmp/gputejas_installation_kit.tar.gz -C .
COPY . gputejas

COPY files/test.cu tmp
RUN update-alternatives --install /usr/bin/gcc gcc /usr/bin/gcc-4.4 44 --slave /usr/bin/g++ g++ /usr/bin/g++-4.4
RUN update-alternatives --install /usr/bin/gcc gcc /usr/bin/gcc-4.8 48 --slave /usr/bin/g++ g++ /usr/bin/g++-4.8
RUN update-alternatives --set gcc /usr/bin/gcc-4.4
#33 1.204 /usr/include/x86_64-linux-gnu/bits/floatn.h(74): error: invalid argument to attribute "__mode__"
#33 1.204 
#33 1.204 /usr/include/x86_64-linux-gnu/bits/floatn.h(86): error: identifier "__float128" is undefined
# Add the following lines to floatn.h
# +#if CUDART_VERSION
# +#undef __HAVE_FLOAT128
# +#define __HAVE_FLOAT128 0
# +#endif
COPY files/floatn.h /usr/include/x86_64-linux-gnu/bits/floatn.h
RUN nvcc -c tmp/test.cu -odir tmp -arch sm_20

# 32 bit
# RUN cp so_files_32bit/libocelot.so '/usr/lib/libocelot.so'
# # RUN rm /usr/lib/i386-linux-gnu/libtinfo.so 2>/dev/null || true
# RUN cp so_files_32bit/libtinfo.so '/usr/lib/i386-linux-gnu/libtinfo.so'
# RUN cp so_files_32bit/libboost_thread.so.1.54.0 '/usr/lib/i386-linux-gnu/libboost_thread.so.1.54.0'
# RUN cp so_files_32bit/libboost_system.so.1.54.0 '/usr/lib/i386-linux-gnu/libboost_system.so.1.54.0'
# RUN cp so_files_32bit/libz.so.1.2.8 '/lib/i386-linux-gnu/libz.so'
# # RUN rm /usr/lib/i386-linux-gnu/libGLEW.so.1.10 2>/dev/null || true
# RUN cp so_files_32bit/libGLEW.so.1.10 '/usr/lib/i386-linux-gnu/libGLEW.so.1.10'
# RUN apt install -y libgl1-mesa-glx

# 64 bit
WORKDIR /home/gputejas
RUN cp so_files_64bit/libocelot.so /usr/lib/libocelot.so
# RUN rm /usr/lib/x86_64-linux-gnu/libtinfo.so 2>/dev/null || true
RUN cp so_files_64bit/libtinfo.so /usr/lib/libtinfo.so
RUN cp so_files_64bit/libboost_thread.so.1.54.0 /usr/lib/libboost_thread.so.1.54.0
RUN cp so_files_64bit/libboost_system.so.1.54.0 /usr/lib/libboost_system.so.1.54.0
RUN cp so_files_64bit/libz.so.1.2.8 /lib/libz.so
# RUN rm /usr/lib/x86_64-linux-gnu/libGLEW.so.1.10 2>/dev/null || true
RUN cp so_files_64bit/libGLEW.so.1.10 /usr/lib/libGLEW.so.1.10
RUN apt install -y libgl1-mesa-glx
WORKDIR /home

WORKDIR /home/gputejas/gputejas
RUN ant clean
RUN ant
RUN ant make-jar
WORKDIR /home

WORKDIR /home/gputejas
RUN g++-4.8 -std=c++0x Tracegen.cpp -c -I .
RUN g++-4.8 -o tracegen ../tmp/*.o Tracegen.o -locelot -ltinfo
RUN ./tracegen 192
RUN mkdir 192
RUN mv *.txt 192
RUN java -jar gputejas/jars/Tracesimplifier.jar gputejas/src/simulator/config/config.xml tmp . 1
RUN java -jar gputejas/jars/GPUTejas.jar gputejas/src/simulator/config/config.xml 192.txt . 1
WORKDIR /home

RUN apt clean
RUN rm -rf tmp

CMD ["/bin/bash", "-c", "gcc-4.4 --version && gcc-4.8 --version && g++-4.4 --version && g++-4.8 --version && java -version && ant -version && nvcc --version"]
