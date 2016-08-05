#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/types.h>
#include <dirent.h>
#include <unistd.h>
#include <errno.h>


int main (int argc, char *argv[]) 
{

  char st[] ="Where_there_is will_there_is a way.";
  char *ch;
  printf("Split \"%s\"\n", st);
  ch = strtok(st, "_");
  while (ch != NULL) {
  printf("%s\n", ch);
  ch = strtok(NULL, "_");
  }

    DIR* FD;
    struct dirent* in_file;
    FILE    *common_file;
    FILE    *entry_file;
    char    buffer[BUFSIZ];
    char in_dir [] = "periodic/";
    char delim = ',';
    char * token;

    if (NULL == (FD = opendir (in_dir))) 
    {
        fprintf(stderr, "Error : Failed to open input directory - %s\n", strerror(errno));

        return 1;
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
        /* Open directory entry file for common operation */
        /* TODO : change permissions to meet your need! */
        char outFile [50]; // for outputFile
        // token = strtok(in_file->d_name, "_");
        // token = strtok(NULL, "_");
        // char* file_counter = strtok(NULL, "_.");
        // printf("%s\n",token);
        sprintf(outFile,"%s%s",in_dir,in_file->d_name);  
        printf("out = %s\n",outFile); 
    }

    /* Don't forget to close common file before leaving */
    //fclose(common_file);

    return 0;
}