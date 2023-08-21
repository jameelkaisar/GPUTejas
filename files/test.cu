#include <iostream>

__global__ void vectorAdd(int *a, int *b, int *c, int n) {
    int i = blockIdx.x * blockDim.x + threadIdx.x;
    if (i < n) {
        c[i] = a[i] + b[i];
    }
}

int GPUTejas_main(int argc, char **argv) { // Renamed main function
    int n = 1024;
    int *a, *b, *c;
    int *d_a, *d_b, *d_c;

    // Memory allocation and initialization code...
    a = new int[n];
    b = new int[n];
    c = new int[n];

    for (int i = 0; i < n; ++i) {
        a[i] = i;
        b[i] = 2 * i;
    }

    cudaMalloc((void**)&d_a, n * sizeof(int));
    cudaMalloc((void**)&d_b, n * sizeof(int));
    cudaMalloc((void**)&d_c, n * sizeof(int));

    cudaMemcpy(d_a, a, n * sizeof(int), cudaMemcpyHostToDevice);
    cudaMemcpy(d_b, b, n * sizeof(int), cudaMemcpyHostToDevice);

    vectorAdd<<<(n + 255) / 256, 256>>>(d_a, d_b, d_c, n);

    cudaMemcpy(c, d_c, n * sizeof(int), cudaMemcpyDeviceToHost);

    // Cleanup and deallocation code...
    delete[] a;
    delete[] b;
    delete[] c;
    cudaFree(d_a);
    cudaFree(d_b);
    cudaFree(d_c);

    return 0;
}
