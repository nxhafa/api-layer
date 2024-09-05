module.exports = {
    globalExtends: ["config:recommended", ":gitSignOff"], // using this instead of "extends" solves the problem with order of the configuration
    repositories: ['nxhafa/api-layer'],
    baseBranches: ["updatingRenovate"],
    dependencyDashboard: true,
    includePaths: ["api-catalog-ui/frontend/**"],
    packageRules: [
        {
            //for v2.x.x branch ignore grouping from extends preset, find all packages which are patches,
            // slug them and make PR with name "all patch dependencies"
            "matchBaseBranches": ["updatingRenovate"],
            "matchPackageNames": ["@emotion/react"],
            "groupName": "all patch dependencies",
            "groupSlug": "all-patch",
            "matchUpdateTypes": ["patch"],
        },
    ],
    hostRules: [
        {
            "hostType": "npm",
            "matchHost": "https://zowe.jfrog.io/artifactory/api/npm/npm-org/"
        }
    ],
    printConfig: true,
    labels: ['dependencies'],
    dependencyDashboardLabels: ['dependencies'],
    commitMessagePrefix: 'chore: ',
    prHourlyLimit: 0, // removes rate limit for PR creation per hour
    npmrc: 'legacy-peer-deps=true\nregistry=https://zowe.jfrog.io/artifactory/api/npm/npm-org/', //for updating lock-files
    npmrcMerge: true //be combined with a "global" npmrc
};
