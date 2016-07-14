/* morph1.c */

#include <stdio.h>
#include <string.h>
#include <math.h>
#include <malloc.h>
#include <stdint.h>



/******************************************/
/* Program for binomial file morphing     */
/*                                        */
/*   This program creates a morph of two  */
/*   files by a random process. Length of */ 
/*   output file is the minimum length of */
/*   the input files. Use 1 as default    */
/*   for skip, or other values for        */
/*   additional random experiments. The   */
/*   program moves between 2 states, 0    */
/*   and 1, according to Markov process.  */
/*   Whenever in state 0, a character is  */
/*   taken from first file, otherwise, a  */
/*   character is taken from second file. */
/*                                        */
/* Command line arguments:                */
/*                                        */
/* #1 skips for random number generator   */
/*    (any number between 0 and 1000)     */ 
/*                                        */
/* #2 probability p of staying in state 0 */
/*                                        */
/* #3 probability q of staying in state 1 */
/*                                        */
/* #4 name of first input file            */
/*                                        */
/* #5 name of second input file           */
/*                                        */
/* #6 name of output file to be created   */
/*                                        */
/******************************************/

int main (int argc, char *argv[]) 
{
  double p, q;               /* state change probabilities */
  int skip;                  /* skip for random number generator */
  double msrandom();
  FILE *if1, *if2, *of;
  char *inFile1;             /* input file 1 */
  char *inFile2;             /* input file 2 */
  char *outFile;             /* output file */
  char c1, c2;
  int l1, l2, l;
  int i;
  int seed;
  //int32_t seed;
  double v;
  double test;
  int state;     /* 0=copy and 1=replace */

  seed = 123;


 if (argc != 7)  error("Incorrect number of command line parameters!\n");

  skip = atoi(argv[1]);
  //printf("Prob = %s " , argv[2]);
  
  p = atof(argv[2]);
  //printf("Testing\n");

  q = atof(argv[3]);
    printf("The probability = %f\n %f\n%d\n",p,q,skip);
  inFile1 = argv[4];
  inFile2 = argv[5];
  outFile = argv[6];
  // inFile1 = argv[1];
  // inFile2 = argv[2];
  // outFile = argv[3];
  // scanf("%d %lf %lf",&skip,&p,&q);
  printf("The probability = %f\n",p);

  /* try to open input files */
  if (((if1 = fopen(inFile1, "r")) == NULL) || 
      ((if2 = fopen(inFile2, "r")) == NULL))
    error("Input file could not be opened!\n");

  /* try to open output file */
  if ((of = fopen(outFile, "w")) == NULL)
    error("Output file could not be opened!\n");

  state = 0;
  while((feof(if1) == 0) && (feof(if2) == 0))
  {
    /* state change */
    for (i = 0; i < skip; i++)  v = msrandom(&seed);
    v = msrandom(&seed);
    int val = v > p;


    //  printf("Random number generated: %f >? %f = %d\n",v,p,val);
    if (state == 0)
    {

      if (val == 1)
        state = 1;
    }
    else
    {
    

           //printf("In here %d\n",state);

      if (v > q)
        state = 0;
    }
    
    l1 = fread((void *)(&c1), sizeof(char), 1, if1);
    l2 = fread((void *)(&c2), sizeof(char), 1, if2);
    l = (l1<l2)? l1:l2;
    if (state == 0)
      fwrite((void *)(&c1), sizeof(char), l, of);
    else
      fwrite((void *)(&c2), sizeof(char), l, of);
  }
      
  fclose(if1);
  fclose(if2);
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


