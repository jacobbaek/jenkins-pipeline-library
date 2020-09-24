#!/usr/bin/env groovy
def call(Map<String,String> vmIPs, String provider) {
  fetchCloudsConf()
  vmIPs.each { name, net_ip ->
    port_id = sh(returnStdout: true,
                 script: "openstack port list --os-cloud ${provider} | grep \\'${net_ip}\\' | awk -F \"|\" '{ print \$2 }'")
    println("Found port: ${port_id} from instance ${name}. Disabling the port...")
    sh """
      openstack port set --no-security-group --os-cloud ${provider} ${port_id}
      openstack port set --disable-port-security --os-cloud ${provider} ${port_id}
    """
  }
}
