import requests
import urllib2
import wget
import string
from datetime import datetime
import os


'''

	We have all the urls on the harddisk ( ran getUrls first) and now will get versions of this
	url from the wayback machine using there api.

'''

file_name = 'Internet_Archive_DataSet.txt'
traversed_name = 'traversed_list.txt'
url_list = [] # hold the urls
#traversed_list = {} # this holds all the urls we have finished, if program crashes etc
# These are the dates we will be retrieving from the wayback api
# today = '20160625/'
# last_week = '20160618/'
# last_month = '20160525/'
# last_year = '20150625/'
# directory = 'datasets/' # this is the dir where we will store all the documents that we download

today = '20160710/'
six_month = '20160110/'
last_year = '20150710/'
two_years = '20140710/'
directory = 'datasets2/' # this is the dir where we will store all the documents that we download


# variables for the wayback api
base_domain = 'http://web.archive.org/web/' # this is the base domain


# Open the url list and read it to memory
def read_file():

	# # open the traversed list and store it in the dictionary, so this way we can access if we have 
	# # seen this url
	# with open(traversed_name,'r') as file:
	# 	for line in file:
	# 		if line.strip('\n'):
	# 			traversed_list[line.strip('\n')] = (line.strip('\n'))

	#open file for reading
	with open(file_name,'r') as file:
		# loop through each line and only add it ifs not an empty line and we have not seen it already
		for line in file:
			if line.strip('\n'):
				url_list.append(line.strip('\n')) # add the word to the list



# This opens a connection with the wayback and access the three versions for the given url and downloads
# them for the given url
def get_versions(url):
	# first we will make a directory for this  with the url name
	t = url.strip('http://')
	t =url.strip('https://') # this accidentially creates a new a dir with just http
	path = directory + t
	if not os.path.exists(path):
		os.makedirs(path) # make a dir for this url
		path = path + '/' # get the full path to this folder now

		# current_version = base_domain + today + url # get the current version
		# last_week_version = base_domain + last_week + url # get last weeks version
		# last_month_version = base_domain + last_month + url # get last months version
		# last_year_version = base_domain+last_year + url # get last years version

		# wget.download(current_version,path + 'current.html') # download current version
		# wget.download(last_week_version,path + 'last_week.html') # download last_weeks version
		# wget.download(last_month_version,path+'last_month.html') # download last months version
		# wget.download(last_year_version, path + 'last_year_version.html') # download last years version


		current_version = base_domain + today + url # get the current version
		six_month_version = base_domain + six_month + url # get last weeks version
		last_year_version = base_domain + last_year + url # get last months version
		two_year_version = base_domain+two_years + url # get last years version

		wget.download(current_version,path + 'current.html') # download current version
		wget.download(six_month_version,path + 'six_month.html') # download last_weeks version
		wget.download(last_year_version,path+'last_year.html') # download last months version
		wget.download(two_year_version, path + 'two_years.html') # download last years version
		
		#traversed_list[url] = url # finally add list to the 
	else:
		print 'error! Url Already exists ' + url




#parse input string
def main():
	read_file() # read the urls
	# #print (url_list)
	for url in url_list:
		get_versions(url) # get the versions for all the urls







main()






















# cnn = "http://web.archive.org/web/timemap/json/http://www.cnn.com/" # get all cnn pages
# test = "<http://web.archive.org/web/20160723163109/http://www.cnn.com/>; rel=last memento; datetime=Thu, 23 Jun 2016 16:31:09 GMT"
# y = 'https://sg.celebrity.yahoo.com/'
# test_url = 'http://web.archive.org/web/20150625/http://www.cnn.com/'

#splitted = re.search(r'\<(.*)\>', r.text).group(1)
# splitted = re.findall(r'\<([^]]*)\>',r.text)
#splitted=re.split(r'\<(.*)\>',test) # the url is inside the <> brackets
# print (splitted)
# i = 0
# print len(splitted)
# while i < 20:
# 	print splitted[i]
# 	i+=1


# s = r[3]
# print s
# arr = s.split(";")
# parsed =arr[1]
# identity = string.maketrans("", "")
# parsed = parsed.translate(identity,"<>")
# #parsed = re.sub('<>','',parsed) # replacing all the brackets
# print parsed

	

# download = "http://web.archive.org/web/20160622133103/http://www.cnn.com/"
# response = urllib2.urlopen(download)
# html = response.read()



#downloads the pages
#wget.download(download)
#print r.text


# dates
#d = datetime.strptime('20000623163109',"%Y%m%d%H%M%S");
# d2 = datetime.now()
# delta = d2-d
# print delta.days