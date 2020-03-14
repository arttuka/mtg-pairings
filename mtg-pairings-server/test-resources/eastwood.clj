(disable-warning
 {:linter                      :redefd-vars
  :if-inside-macroexpansion-of #{'mount.core/defstate}
  :within-depth                6
  :reason                      "defstate generates multiple defs"})
