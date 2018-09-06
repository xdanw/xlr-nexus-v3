###################################################################################
#  Name: Nexus Published Artifact Trigger
#
#  Description: Trigger new release when new version of artifact is published to the Sonatype Nexus repository
#  Implementation:
#       Pull Nexus REST API to resolve an artifact identified by GAV coordinates given, and retrieve a set of details about that artifact.
#       Detected version (version and snapshotBuildNumber for snapshots) is stored into the triggerState and output variable.
#       REST API documentation: https://oss.sonatype.org/nexus-restlet1x-plugin/default/docs/path__artifact_maven_resolve.html
###################################################################################
import sys, string, urllib
import com.xhaus.jyson.JysonCodec as json

RESOLVE_PATH = 'service/rest/v1/search'
RESOLVE_PARAMETERS = {
    'repository': repository,
    'maven.groupId': maven_groupId,
    'maven.artifactId': maven_artifactId,
    'maven.baseVersion': maven_baseVersion
}

if server is None:
    print "No Nexus server provided."
    sys.exit(1)

request = HttpRequest(server, username, password)
params = dict((k, v) for k, v in RESOLVE_PARAMETERS.items() if v)
context = "%s?%s" % (RESOLVE_PATH, urllib.urlencode(params))
response = request.get(context, contentType = 'application/json')

if not response.isSuccessful():
    if response.status == 404 and triggerOnInitialPublish:
        print "Artifact '%s:%s:%s' not found in repository '%s'. Ignoring." % (maven_groupId, maven_artifactId, maven_baseVersion, repository)
        # the following initialisation is to enable a scenario where we wish
        # to trigger a release on a first publish of an artifact to Nexus
        if not triggerState:
            artifactVersion = triggerState = '0.0.0'
    else:
        print "Failed to fetch artifact metadata from Nexus repository %s" % server['url']
        response.errorDump()
        sys.exit(1)
else:
    resolution = json.loads(response.response)
    assets = resolution.get('assets')
    version = str(resolution.get('version'))
    triggerState = version

    # populate output variables
    assetVersion = version
    assetRepository = str(assets.get('repository')) if assets.get('repository') else ''
    assetPath = str(assets.get('path')) if assets.get('path') else ''
    assetFormat = str(assets.get('format')) if assets.get('format') else ''
    assetDownloadUrl = str(assets.get('downloadUrl')) if assets.get('downloadUrl') else ''
