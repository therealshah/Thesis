/* rando.c */

#include <stdio.h>
#include <string.h>
#include <math.h>
#include <malloc.h>


/******************************************/
/* Program for random file creation       */
/*                                        */
/* Command line arguments:                */
/*                                        */
/* #1 skips for random number generator   */
/*    (any number between 0 and 1000)     */
/*                                        */
/* #2 size of random file                 */
/*                                        */
/* #3 name of output file                 */
/*                                        */
/******************************************/

int main (int argc, char *argv[]) 
{
  int skip;                  /* skip for random number generator */
  double msrandom();
  FILE *of;
  char *outFile;            /* output file */
  int i, j, n;
  int seed;
  double v;
  int asc;
  char c;
  int periodic = 20;
  char content [periodic];


  seed = 123;

  if (argc != 4)  error("Incorrect number of command line parameters!\n");

  skip = atoi(argv[1]);
  n = atoi(argv[2]);
  outFile = argv[3];

  /* try to open output file */
  if ((of = fopen(outFile, "w")) == NULL)
    error("Output file could not be opened!\n");


  for (i = 0; i < periodic;++i){
    for (j = 0; j < skip; j++)  v = msrandom(&seed);
    asc = (int)(256.0 * msrandom(&seed));
    c = (char)(asc);
    content[i] = c;
  }

  for (i = 0; i < n; i+= periodic){
    fwrite(content, sizeof(char), sizeof(content), of);
  }
      
  fclose(of);
}


/********************************************/
/* generate a random double between 0 and 1 */
/********************************************/

double msrandom(int *seed)

{
  int lo;
  int hi;
  int test;

  hi=(*seed)/127773;
  lo=(*seed) % 127773;
  test=16807*lo-2836*hi;
  if (test>0) *seed=test;
  else *seed=test+2147483647;
  return((double)(*seed)/(double)2147483647);
}


/*********************************/
/* Print error message and abort */
/*********************************/

error(char *text)

{
  printf(text);
  exit(0);
}


