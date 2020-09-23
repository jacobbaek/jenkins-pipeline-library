#!/usr/bin/env groovy
def call(String namePrefix, String networkType="mgmt", String provider='taco-prod') {
  fetchCloudsConf()

	def result = [:]
	infos = sh(returnStdout: true, script: "openstack server list --os-cloud ${provider} -f json | jq --raw-output 'sort_by(.Name) | .[] | select(.Name|test(\"${namePrefix}\")) | .Name, .Networks'").split('\n')
	print infos
	for (i = 0; i < infos.size(); i=i+2) {
	  infos[i+1].split(";").each { it ->
      if(it.contains(networkType)) {
				ip = it.split("=")[-1]
	  		result[infos[i]] = ip
			}
		}
	}
	return result
}
