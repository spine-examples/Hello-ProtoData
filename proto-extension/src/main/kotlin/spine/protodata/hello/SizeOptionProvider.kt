package spine.protodata.hello

import com.google.auto.service.AutoService
import com.google.protobuf.ExtensionRegistry
import io.spine.option.OptionsProvider
import io.spine.protodata.hello.SizeOptionProto

@AutoService(OptionsProvider::class)
public class SizeOptionProvider : OptionsProvider {

    override fun registerIn(registry: ExtensionRegistry) {
        SizeOptionProto.registerAllExtensions(registry)
    }
}
