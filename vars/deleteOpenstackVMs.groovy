#!/usr/bin/env groovy

def call(String namePrefix, String provider) {
  println("Start deleting Openstack VMs...")
  fetchCloudsConf()
	sh """
		openstack server list --os-cloud ${provider} | grep ${namePrefix} | awk '{print \$2}' | xargs openstack server delete --os-cloud ${provider} --wait
    VOL_CNT=\$(openstack volume list --os-cloud ${provider} -f value | grep ${namePrefix} | wc -l)
    SNAP_CNT=\$(openstack volume snapshot list --os-cloud ${provider} -f value --volume ${namePrefix} | wc -l)
    if [ \$VOL_CNT -ne 0 ] && [ \$SNAP_CNT -eq 0 ]; then
      openstack volume list --os-cloud ${provider} | grep ${namePrefix} | awk '{print \$2}' | xargs openstack volume delete --os-cloud ${provider} --force || true
    fi
  """
  println("Deleting Openstack VM has been finished!")
}
