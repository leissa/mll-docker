#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

void mll(double* restrict in, double* restrict out);

void print_array(int n, double* a) {
    const char* sep = "";
    printf("(");
    for (int i = 0; i != n; ++i) {
        printf("%s%f", sep, a[i]);
        sep = ", ";
    }
    printf(")\n");
}

int main(int argc, char** argv) {
    bool backwards = false;

    double inputs[argc];
    memset(inputs, 0, sizeof(double) * argc);

    int num_ins = 0;
    for (int i = 1; i != argc; ++i) {
        if (strcmp(argv[i], "-b") == 0) {
            backwards = true;
        } else {
            inputs[num_ins++] = atof(argv[i]);
        }
    }

    size_t num_outs = backwards ? num_ins + 1 : 1;
    double outputs[num_outs];
    memset(outputs, 0, sizeof(double) * num_outs);

    printf("mll");
    print_array(num_ins, inputs);
    mll(inputs, outputs);
    printf("=> ");
    print_array(num_outs, outputs);

    return EXIT_SUCCESS;
}
