
import {H2} from '@jetbrains/ring-ui/components/heading/heading'

import {React} from "@jetbrains/teamcity-api"
import type {PluginContext} from "@jetbrains/teamcity-api";

import styles from './App.css'

const ProfileInfo = React.memo(({onNameClick, name}) =>
    <H2 className={styles.name} onClick={onNameClick}>{`Hello, ${name}`}</H2>)

const defaultProfile = {
    name: "Elvis",
}

function App({location}: {| location: PluginContext |}): React.Node {
    const [expanded, setExpanded] = React.useState(false)
    const toggleExpanded = React.useCallback(() => setExpanded(state => !state), [])

    return (
        <div className={styles.wrapper}>
            <ProfileInfo onNameClick={toggleExpanded} name={defaultProfile.name} />
            {expanded && <div>
                {Object.entries(location).map(
                    ([key, value]) =>
                        value != null && typeof value === 'string' ? <p key={key}>{`${key}:${value}`}</p>: null
                )}
            </div>}
        </div>
    )
}

export default App
