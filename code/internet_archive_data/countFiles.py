import os
import shutil


'''

	Once we get our 5000 URLS, we make sure each url has 4 distinct versions. If it does not, we filter it out
'''


dir_count = 0
same_count = 0
file_count = 0 # count total files
# This method, given the pathn to the folder, wil check if the folder has 4 versions of that url
# if not, delete that directory
def check_directories(path):
	global dir_count
	global file_count
	#if len(os.listdir(path)) != 3:
	dir_count += 1
	file_count += len(os.listdir(path))


# This methid removes any files that were corrupted during downloading ( meanign they were not downloaded properly)
def remove_corrupted_files(path):
	total_size = 0
	for dirpath, dirnames, filenames in os.walk(path): 
		if len(filenames) != 4: # if dir doesn't have all 4 versions, delete it
			print dirpath
			#shutil.rmtree(dirpath)  # uncomment to remove
			break
		for f in filenames: # for every file
			#print f
			fp = os.path.join(dirpath, f)
			total_size += os.path.getsize(fp)
			if os.path.getsize(fp) == 0:
				print path
				#shutil.rmtree(path)  # uncomment to remove
				break

		#shutil.rmtree(path)


# This checks if the previous version of the file is the same as the current one. If yes, remove it
def file_filter(path):
	global same_count
	for dirpath, dirnames, filenames in os.walk(path):
		#print (sorted(filenames))
		filenames = sorted(filenames)
		current_version = open(path + '/' + filenames[0],'r') # current version
		six_month_version = open(path + '/' +filenames[2],'r') # six month
		last_year_version = open(path + '/' +filenames[1],'r') # last year
		two_year_version = open(path + '/' +filenames[3],'r') # two years

		curr = current_version.read()
		six_month = six_month_version.read()
		last_year = last_year_version.read()
		two_year = two_year_version.read()

		#  if either of the versions are the same, delete that directory
		if (curr == six_month or six_month == last_year or last_year == two_year or curr == last_year or curr == two_year 
			or six_month == two_year  ):
			# remove this dir
			#shutil.rmtree(dirpath)  # uncomment to remove
			same_count+=1
		current_version.close()
		six_month_version.close()
		last_year_version.close()
		two_year_version.close()

		break

		#shutil.rmtree(path)





directory = '../path/to/dir'
sub_dirs =  next(os.walk(directory))[1] # get all the directories for this file
for folder in sub_dirs:
	check_directories(directory + folder + '/')
	remove_corrupted_files(directory + folder + '/')
	#file_filter(directory + folder + '/') # FILTER all the same versions
	#break

print 'total # directories = ',dir_count
print 'total number of version = ',file_count
print 'total number of same versions = ',same_count