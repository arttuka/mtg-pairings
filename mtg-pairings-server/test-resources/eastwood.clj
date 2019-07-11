(disable-warning
 {:linter                      :redefd-vars
  :if-inside-macroexpansion-of #{'mount.core/defstate}
  :within-depth                6
  :reason                      "defstate generates multiple defs"})

(disable-warning
 {:linter                      :constant-test
  :if-inside-macroexpansion-of #{'korma.core/aggregate}
  :within-depth                6
  :reason                      "aggregate generates constant tests"})
