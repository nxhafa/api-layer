module.exports = {
    globalExtends: ["config:recommended"], // using this instead of "extends" solves the problem with order of the configuration
    repositories: ['nxhafa/api-layer'],
    baseBranches: ["updateRenovate"],
    dependencyDashboard: true,
    includePaths: ["zowe-cli-id-federation-plugin/**"],
    allowedPostUpgradeCommands: ['npm install'],
    commitBody: "Signed-off-by: {{{gitAuthor}}}",
    packageRules: [
        {
            //for updateRenovate branch find all packages which are minor and patches,
            // slug them and make PR with name "all non-major dependencies"
            "matchBaseBranches": ["updateRenovate"],
            "groupName": "all non-major dependencies",
            "groupSlug": "all-minor-patch",
            "matchPackageNames": ["*"],
            "matchUpdateTypes": ["minor", "patch"],
            "postUpgradeTasks": {
                "commands": ["npm install"],
                "fileFilters": ["**/**"],
                "executionMode": "branch"
            }
        },
        {
            //for updateRenovate make dashboard approval to all major dependencies updates
            "matchBaseBranches": ["updateRenovate"],
            "matchUpdateTypes": ["major"],
            "dependencyDashboardApproval": true,
        }
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
    ignoreDeps: ['history', 'jsdom', 'react-router-dom', '@mui/icons-material', '@mui/material', '@material-ui/core', '@material-ui/icons', 'undici'],
    commitMessagePrefix: 'chore: ',
    prHourlyLimit: 0, // removes rate limit for PR creation per hour
    npmrc: 'legacy-peer-deps=true\nregistry=https://zowe.jfrog.io/artifactory/api/npm/npm-org/', //for updating lock-files
    npmrcMerge: true //be combined with a "global" npmrc
};
