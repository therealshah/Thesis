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
/* #2 size of random file ( guarenttes atleast this much) */
/*                                        */
/* #3 name of output file                 */
/*      
/* #4 length of the periodic data         */
/******************************************/

/*

  This rando code generates periodic files.
  The main two parameters for this is the content that is being repeated and the length the content ( that is repeated)
*/

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
  int periodic_length;


  seed = 123;

  if (argc != 5)  error("Incorrect number of command line parameters!\n");

  skip = atoi(argv[1]);
  n = atoi(argv[2]);
  outFile = argv[3];
  periodic_length = atoi(argv[4]);

  /* try to open output file */
  if ((of = fopen(outFile, "w")) == NULL)
    error("Output file could not be opened!\n");


  // first generate a actual text that will be repeated

  char content [periodic_length]; // hold the content

  for (i = 0; i < periodic_length ; i++)
  {
    for (j = 0; j < skip; j++)  v = msrandom(&seed);
    asc = (int)(256.0 * msrandom(&seed));
    c = (char)(asc);
    content[i] = c; // store the conent
    //fwrite((void *)(&c), sizeof(char), 1, of);
  }

  // write thr content to the file
  for (i = 0; i < n; i+=periodic_length)
      fwrite(content, sizeof(char), sizeof(content), of);

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


