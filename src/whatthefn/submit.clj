(ns whatthefn.submit
  (:require [whatthefn.sandbox :as sb]
            [clojure.edn]))

(defn edn-response [data]
  {:body (pr-str data)
   :headers {"Content-Type" "application/edn;charset=UTF-8"}})

(defn str-reader [text]
  (java.io.PushbackReader. (java.io.StringReader. text)))

(defn edn-seq [reader]
  (when-let [next-val (clojure.edn/read {:eof nil} reader)]
    (cons next-val (edn-seq reader))))


(defn eval-code [sandbox code]
  (let [forms (edn-seq (str-reader code))
        results (map sandbox forms)
        result-text (clojure.string/join "\n" (map pr-str results))]
    result-text))


(defn submit-fn [code]
  (try
    (edn-response {:result (eval-code (sb/sandbox) code)})
    (catch Exception e
      (edn-response {:result (.getMessage e)}))))


(defn submit-value [value-str]
  (let [sandbox (sb/sandbox)]
    (sandbox '(def the-fn whatthefn.functions/fib))

    (try
      (edn-response {:result (sandbox (list 'the-fn (clojure.edn/read-string value-str)))})
      (catch Exception e
        (edn-response {:result (.getMessage e)})))))


(defn submit-repl [req code]
  (let [session (:session req)
        repl (or (:repl session) (sb/sandbox))
        new-sesion (assoc session :repl repl)]
    (try
      (-> (edn-response {:result (eval-code repl code)})
          (assoc :session new-sesion))
      (catch Exception e
        (edn-response {:result (.getMessage e)})))))