#include <stdio.h>
#include <string.h>
#include <math.h>
#include <malloc.h>
#include <stdint.h>
#include <stdlib.h>
#include <sys/types.h>
#include <dirent.h>
#include <unistd.h>
#include <errno.h>




/******************************************/
/* Program for binomial file morphing     */
/*                                        */
/*   This program creates a morph of two  */
/*   periodic files by a random process. 
/*   This is very similar to the original */  
/*   morph code with subtle differences.   */ 
/*                                        */
/* Command line arguments:                */
/*                                        */
/*                                        */
/*                                        */
/*                                        */
/* #1 name of second input file           */
/*                                        */
/*                                        */
/******************************************/

int main (int argc, char *argv[]) 
{
  double p, q;               /* state change probabilities */
  int skip;                  /* skip for random number generator */
  double msrandom();
  FILE *if1, *if2, *of;          
  char *inFile2;             /* input file 2 */
  char c1, c2;
  int l1, l2, l;
  int i;
  int seed;
  double v;
  int state;     /* 0=copy and 1=replace */
  DIR* FD;
  struct dirent* in_file;
  char in_dir [] = "periodic/"; // where all the files are to be morphed with

  seed = 123;


  if (argc != 2)  error("Incorrect number of command line parameters!\n");

  inFile2 = argv[1]; // read the file we will be morphing with

  // read in number of skips, prop of p and prop of q
  printf("Please Eneter the number of skips, probability of staying in file 1 and probability of staying in file 2:" );
  scanf("%d %lf %lf",&skip,&p,&q);
  printf("The probability of staying in file 1 = %f\n",p);
  printf("The probability of staying in file 2 = %f\n",q);

 // read all the files in the directory
  /* Scanning the in directory */
  if (NULL == (FD = opendir (in_dir))) 
  {
      fprintf(stderr, "Error : Failed to open input directory - %s\n", strerror(errno));
  }
  while ((in_file = readdir(FD))) 
  {
      /* On linux/Unix we don't want current and parent directories
       * On windows machine too, thanks Greg Hewgill
       */
      if (!strcmp (in_file->d_name, "."))
          continue;
      if (!strcmp (in_file->d_name, ".."))    
          continue;
      // print the file
    //printf("%s\n",in_file->d_name);
    char inFile1 [25];
    sprintf(inFile1,"%s%s",in_dir,in_file->d_name); 
    /* try to open input files */
    if (((if1 = fopen(inFile1, "r")) == NULL) || 
        ((if2 = fopen(inFile2, "r")) == NULL)){
        printf("%s, %s\n",inFile1,inFile2);
        error("Input file could not be opened!\n");
    }


    // extract the periodic value for this file, which determines which folder this morphed file will go in
    char * c_value = strtok(in_file->d_name, "_"); // read in garabe value
    c_value = strtok(NULL, "_"); // its the second value returned (filename_c_value_fileNum) - we want the c_val
    char * file_num = strtok(NULL, "_."); // separated by _ and .
    char outFile [50]; // for outputFile
    sprintf(outFile,"periodic_%s/pOut_%s_%s.txt",c_value,c_value,file_num);  // name is periodic_dirNum(periodic_val)/pOut_periodicValae_filNum
    printf("%s\n",outFile);
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
    for (i = 0; i < skip; i++)  v = msrandom(&seed);
    skip = (int) (500 * msrandom(&seed) ); // change the new skip
  } // end of the while looop that reads all the files from a dir
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
  printf("%s",text);
  exit(0);
}

