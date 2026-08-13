(ns katamorph.schema.core
  "Portable schema mechanics. Domain registries and kind inference stay outside."
  (:require [katamorph.schema.kind :as kind]
            [katamorph.schema.registry :as registry]
            [katamorph.schema.validation :as validation]))

(def normalize-kind kind/normalize-kind)
(def schema-for registry/schema-for)
(def registry-conflicts registry/registry-conflicts)
(def compose-registries registry/compose-registries)
(def validation-errors validation/validation-errors)
(def validate-schema validation/validate-schema)
(def validate validation/validate)
(def valid? validation/valid?)
